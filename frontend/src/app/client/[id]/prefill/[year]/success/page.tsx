'use client';

import { useEffect, useState } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { PrefillResponse } from '@/lib/api';

export default function PrefillSuccessPage() {
  const router = useRouter();
  const params = useParams();
  const clientId = Number(params.id);
  const year = params.year as string;

  const [result, setResult] = useState<PrefillResponse | null>(null);
  const [rawJson, setRawJson] = useState<string>('');

  useEffect(() => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('token');
      if (!token) {
        router.push('/login');
        return;
      }

      const stored = sessionStorage.getItem('prefillResult');
      const storedJson = sessionStorage.getItem('rawPrefillJson');
      if (stored) {
        setResult(JSON.parse(stored));
        if (storedJson) setRawJson(storedJson);
      } else {
        router.push(`/client/${clientId}/prefill/${year}`);
      }
    }
  }, [clientId, year, router]);

  const handleDownloadJson = () => {
    if (rawJson) {
      const blob = new Blob([rawJson], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `prefill_${result?.pan || 'data'}_${year}.json`;
      a.click();
      URL.revokeObjectURL(url);
    }
  };

  if (!result) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-50 to-blue-50 flex items-center justify-center px-4">
      <div className="max-w-2xl w-full">
        <div className="card text-center">
          {/* Success Icon */}
          <div className="inline-flex items-center justify-center w-20 h-20 rounded-full bg-green-100 mb-6">
            <svg className="w-10 h-10 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
            </svg>
          </div>

          <h1 className="text-2xl font-bold text-gray-900 mb-2">Prefill Uploaded Successfully!</h1>
          <p className="text-gray-500 mb-8">
            The prefill data has been validated and stored for {result.clientName}
          </p>

          {/* Client Info */}
          <div className="bg-gray-50 rounded-lg p-4 mb-6 text-left">
            <div className="grid grid-cols-3 gap-4">
              <div>
                <p className="text-xs text-gray-400 font-medium uppercase">Client</p>
                <p className="text-sm font-semibold text-gray-900">{result.clientName}</p>
              </div>
              <div>
                <p className="text-xs text-gray-400 font-medium uppercase">PAN</p>
                <p className="text-sm font-mono font-semibold text-gray-900">{result.pan}</p>
              </div>
              <div>
                <p className="text-xs text-gray-400 font-medium uppercase">Assessment Year</p>
                <p className="text-sm font-semibold text-gray-900">{result.assessmentYear}</p>
              </div>
            </div>
          </div>

          {/* Income Summary */}
          <div className="text-left mb-6">
            <h3 className="font-semibold text-gray-900 mb-3">Extracted Income Summary</h3>
            <div className="space-y-2">
              <div className="flex justify-between py-2 px-3 bg-gray-50 rounded">
                <span className="text-sm text-gray-600">Gross Salary</span>
                <span className="text-sm font-semibold">
                  ₹{result.incomeSummary.grossSalary.toLocaleString('en-IN')}
                </span>
              </div>
              <div className="flex justify-between py-2 px-3 bg-gray-50 rounded">
                <span className="text-sm text-gray-600">Other Sources</span>
                <span className="text-sm font-semibold">
                  ₹{result.incomeSummary.otherSources.toLocaleString('en-IN')}
                </span>
              </div>
              <div className="flex justify-between py-2 px-3 bg-gray-50 rounded">
                <span className="text-sm text-gray-600">Section 80C Deduction</span>
                <span className="text-sm font-semibold text-green-600">
                  -₹{result.incomeSummary.section80C.toLocaleString('en-IN')}
                </span>
              </div>
              <div className="flex justify-between py-2 px-3 bg-gray-50 rounded">
                <span className="text-sm text-gray-600">Section 80D Deduction</span>
                <span className="text-sm font-semibold text-green-600">
                  -₹{result.incomeSummary.section80D.toLocaleString('en-IN')}
                </span>
              </div>
              <div className="flex justify-between py-2 px-3 bg-primary-50 rounded-lg">
                <span className="text-sm font-bold text-primary-900">Total Income</span>
                <span className="text-sm font-bold text-primary-700">
                  ₹{result.incomeSummary.totalIncome.toLocaleString('en-IN')}
                </span>
              </div>
            </div>
          </div>

          {/* Status Badge */}
          <div className="inline-flex items-center px-4 py-2 rounded-full bg-green-100 text-green-700 font-medium text-sm mb-8">
            Status: Draft
          </div>

          {/* Action Buttons */}
          <div className="flex flex-col sm:flex-row gap-3">
            <button
              onClick={() => router.push('/dashboard')}
              className="btn-primary flex-1 flex items-center justify-center gap-2"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                  d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
              </svg>
              Back to Dashboard
            </button>
            <button
              onClick={handleDownloadJson}
              className="btn-secondary flex-1 flex items-center justify-center gap-2"
              disabled={!rawJson}
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                  d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
              </svg>
              Download Prefill JSON
            </button>
          </div>

          {/* Phase 2 Buttons (Disabled) */}
          <div className="mt-6 pt-6 border-t border-gray-100">
            <p className="text-xs text-gray-400 mb-3">Coming in Phase 2</p>
            <div className="flex gap-2">
              <button disabled className="btn-secondary flex-1 opacity-50 cursor-not-allowed text-sm">
                Edit ITR-1 Form
              </button>
              <button disabled className="btn-secondary flex-1 opacity-50 cursor-not-allowed text-sm">
                Calculate Tax
              </button>
              <button disabled className="btn-secondary flex-1 opacity-50 cursor-not-allowed text-sm">
                Export XML
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
