'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { clientApi, prefillApi, ClientResponse, PrefillResponse } from '@/lib/api';
import { useDropzone } from 'react-dropzone';

export default function PrefillUploadPage() {
  const router = useRouter();
  const params = useParams();
  const clientId = Number(params.id);
  const year = params.year as string;

  const [client, setClient] = useState<ClientResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  // JSON paste state
  const [jsonText, setJsonText] = useState('');
  const [jsonError, setJsonError] = useState('');
  const [charCount, setCharCount] = useState(0);

  // File upload state
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  // Preview state
  const [preview, setPreview] = useState<PrefillResponse | null>(null);

  useEffect(() => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('token');
      if (!token) {
        router.push('/login');
        return;
      }
    }

    const fetchClient = async () => {
      try {
        const response = await clientApi.getById(clientId);
        setClient(response.data);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to load client');
      } finally {
        setLoading(false);
      }
    };

    fetchClient();
  }, [clientId, router]);

  // Validate JSON in real-time
  const validateJson = (text: string) => {
    setCharCount(text.length);
    if (!text.trim()) {
      setJsonError('');
      return;
    }
    try {
      JSON.parse(text);
      setJsonError('');
    } catch (e: any) {
      const match = e.message.match(/position (\d+)/);
      if (match) {
        const pos = parseInt(match[1]);
        const lines = text.substring(0, pos).split('\n');
        setJsonError(`JSON syntax error at line ${lines.length}, column ${lines[lines.length - 1].length + 1}`);
      } else {
        setJsonError(`JSON syntax error: ${e.message}`);
      }
    }
  };

  const handleJsonChange = (text: string) => {
    setJsonText(text);
    validateJson(text);
  };

  // File drop handler
  const onDrop = useCallback((acceptedFiles: File[]) => {
    if (acceptedFiles.length > 0) {
      const file = acceptedFiles[0];
      if (file.size > 2 * 1024 * 1024) {
        setError('File size exceeds 2MB limit');
        return;
      }
      setSelectedFile(file);
      setError('');

      // Read file content for preview
      const reader = new FileReader();
      reader.onload = (e) => {
        const content = e.target?.result as string;
        try {
          JSON.parse(content);
          setJsonText(content);
          setJsonError('');
          setCharCount(content.length);
        } catch (err: any) {
          setJsonError('Selected file contains invalid JSON');
        }
      };
      reader.readAsText(file);
    }
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: { 'application/json': ['.json'] },
    maxFiles: 1,
    maxSize: 2 * 1024 * 1024,
  });

  // Upload handler
  const handleUpload = async () => {
    setUploading(true);
    setError('');

    try {
      let response;
      if (selectedFile) {
        response = await prefillApi.uploadFile(clientId, year, selectedFile);
      } else if (jsonText.trim()) {
        response = await prefillApi.uploadJson(clientId, year, jsonText);
      } else {
        setError('Please provide JSON data via file upload or paste');
        setUploading(false);
        return;
      }

      // Navigate to success page with data
      const data = response.data;
      sessionStorage.setItem('prefillResult', JSON.stringify(data));
      sessionStorage.setItem('rawPrefillJson', jsonText);
      router.push(`/client/${clientId}/prefill/${year}/success`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Upload failed. Please check your JSON data.');
    } finally {
      setUploading(false);
    }
  };

  // Parse preview from JSON text
  useEffect(() => {
    if (jsonText.trim() && !jsonError) {
      try {
        const parsed = JSON.parse(jsonText);
        setPreview({
          clientId,
          clientName: client?.name || '',
          pan: client?.pan || '',
          assessmentYear: year,
          status: 'draft',
          incomeSummary: {
            assesseeName: parsed.personalInfo?.name || client?.name || '',
            panNumber: parsed.personalInfo?.pan || client?.pan || '',
            dob: parsed.personalInfo?.dob || '',
            grossSalary: parsed.basicDetails?.salary || 0,
            section80C: Math.min(parsed.deductions?.section80C || 0, 150000),
            section80D: parsed.deductions?.section80D || 0,
            otherSources: parsed.basicDetails?.otherSources || 0,
            totalIncome: Math.max(
              (parsed.basicDetails?.salary || 0) +
              (parsed.basicDetails?.otherSources || 0) -
              Math.min(parsed.deductions?.section80C || 0, 150000) -
              (parsed.deductions?.section80D || 0),
              0
            ),
          },
        });
      } catch {
        setPreview(null);
      }
    } else {
      setPreview(null);
    }
  }, [jsonText, jsonError, client, clientId, year]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <button
            onClick={() => router.push(`/client/${clientId}`)}
            className="flex items-center gap-2 text-gray-600 hover:text-gray-900 transition-colors"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7" />
            </svg>
            Back
          </button>
          <div className="text-center">
            <h1 className="text-lg font-bold text-gray-900">Upload Prefill JSON</h1>
            <p className="text-sm text-gray-500">
              {client?.name} • {client?.pan} • {year}
            </p>
          </div>
          <div className="w-20" />
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 py-8">
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
            <p className="text-red-600">{error}</p>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Left: Upload Methods */}
          <div className="space-y-6">
            {/* Method 1: Copy-Paste */}
            <div className="card">
              <h3 className="font-semibold text-gray-900 mb-3">
                Method 1: Paste JSON from Income Tax Portal
              </h3>
              <div className="relative">
                <textarea
                  className="w-full h-64 input-field font-mono text-sm resize-none"
                  placeholder='Paste your prefill JSON here...&#10;&#10;Example:&#10;{&#10;  "personalInfo": {&#10;    "name": "John Doe",&#10;    "pan": "ABCDE1234F",&#10;    "dob": "1990-01-01"&#10;  },&#10;  "basicDetails": {&#10;    "salary": 800000,&#10;    "assessmentYear": "AY2025-26"&#10;  },&#10;  "deductions": {&#10;    "section80C": 150000,&#10;    "section80D": 25000&#10;  }&#10;}'
                  value={jsonText}
                  onChange={(e) => handleJsonChange(e.target.value)}
                />
                <div className="flex items-center justify-between mt-2">
                  <div>
                    {jsonError && (
                      <p className="text-red-500 text-xs">{jsonError}</p>
                    )}
                    {jsonText && !jsonError && (
                      <p className="text-green-500 text-xs">✓ Valid JSON</p>
                    )}
                  </div>
                  <p className="text-xs text-gray-400">{charCount.toLocaleString()} chars</p>
                </div>
              </div>
            </div>

            {/* Method 2: File Upload */}
            <div className="card">
              <h3 className="font-semibold text-gray-900 mb-3">
                Method 2: Upload JSON File
              </h3>
              <div
                {...getRootProps()}
                className={`border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors ${
                  isDragActive
                    ? 'border-primary-400 bg-primary-50'
                    : selectedFile
                    ? 'border-green-400 bg-green-50'
                    : 'border-gray-300 hover:border-primary-400 hover:bg-gray-50'
                }`}
              >
                <input {...getInputProps()} />
                {selectedFile ? (
                  <div>
                    <svg className="w-10 h-10 text-green-500 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                    </svg>
                    <p className="text-sm font-medium text-green-700">{selectedFile.name}</p>
                    <p className="text-xs text-green-500 mt-1">
                      {(selectedFile.size / 1024).toFixed(1)} KB
                    </p>
                  </div>
                ) : (
                  <div>
                    <svg className="w-10 h-10 text-gray-400 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                        d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                    </svg>
                    <p className="text-sm text-gray-600">
                      {isDragActive ? 'Drop JSON file here' : 'Drag & drop a .json file, or click to browse'}
                    </p>
                    <p className="text-xs text-gray-400 mt-1">Max file size: 2MB</p>
                  </div>
                )}
              </div>
              {selectedFile && (
                <button
                  onClick={() => {
                    setSelectedFile(null);
                    setJsonText('');
                    setCharCount(0);
                  }}
                  className="text-sm text-red-500 hover:text-red-600 mt-2"
                >
                  Remove file
                </button>
              )}
            </div>

            {/* Upload Button */}
            <button
              onClick={handleUpload}
              disabled={uploading || (!jsonText.trim() && !selectedFile) || !!jsonError}
              className="btn-primary w-full flex items-center justify-center gap-2 py-3"
            >
              {uploading ? (
                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white" />
              ) : (
                <>
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                      d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
                  </svg>
                  Upload & Validate Prefill
                </>
              )}
            </button>
          </div>

          {/* Right: Preview Panel */}
          <div className="card h-fit sticky top-8">
            <h3 className="font-semibold text-gray-900 mb-4">Preview</h3>

            {preview ? (
              <div className="space-y-4">
                <div className="bg-primary-50 rounded-lg p-4">
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <p className="text-xs text-primary-600 font-medium">Client</p>
                      <p className="text-sm font-semibold text-primary-900">{preview.clientName}</p>
                    </div>
                    <div>
                      <p className="text-xs text-primary-600 font-medium">PAN</p>
                      <p className="text-sm font-mono font-semibold text-primary-900">{preview.pan}</p>
                    </div>
                    <div>
                      <p className="text-xs text-primary-600 font-medium">Assessment Year</p>
                      <p className="text-sm font-semibold text-primary-900">{preview.assessmentYear}</p>
                    </div>
                    <div>
                      <p className="text-xs text-primary-600 font-medium">Prefill PAN</p>
                      <p className="text-sm font-mono font-semibold text-primary-900">
                        {preview.incomeSummary.panNumber || 'N/A'}
                      </p>
                    </div>
                  </div>

                  {/* PAN match check */}
                  {preview.incomeSummary.panNumber && preview.incomeSummary.panNumber !== preview.pan && (
                    <div className="mt-3 bg-red-100 rounded p-2">
                      <p className="text-xs text-red-700 font-medium">
                        ⚠ PAN Mismatch! Prefill PAN does not match client PAN
                      </p>
                    </div>
                  )}
                </div>

                <div className="space-y-3">
                  <div className="flex justify-between py-2 border-b border-gray-100">
                    <span className="text-sm text-gray-500">Gross Salary</span>
                    <span className="text-sm font-semibold">
                      ₹{preview.incomeSummary.grossSalary.toLocaleString('en-IN')}
                    </span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-gray-100">
                    <span className="text-sm text-gray-500">Other Sources</span>
                    <span className="text-sm font-semibold">
                      ₹{preview.incomeSummary.otherSources.toLocaleString('en-IN')}
                    </span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-gray-100">
                    <span className="text-sm text-gray-500">Section 80C</span>
                    <span className="text-sm font-semibold text-green-600">
                      -₹{preview.incomeSummary.section80C.toLocaleString('en-IN')}
                    </span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-gray-100">
                    <span className="text-sm text-gray-500">Section 80D</span>
                    <span className="text-sm font-semibold text-green-600">
                      -₹{preview.incomeSummary.section80D.toLocaleString('en-IN')}
                    </span>
                  </div>
                  <div className="flex justify-between py-2 bg-gray-50 rounded-lg px-3">
                    <span className="text-sm font-semibold text-gray-900">Total Income</span>
                    <span className="text-sm font-bold text-primary-700">
                      ₹{preview.incomeSummary.totalIncome.toLocaleString('en-IN')}
                    </span>
                  </div>
                </div>
              </div>
            ) : (
              <div className="text-center py-12 text-gray-400">
                <svg className="w-12 h-12 mx-auto mb-3 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                    d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <p className="text-sm">Paste or upload JSON to see preview</p>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}
