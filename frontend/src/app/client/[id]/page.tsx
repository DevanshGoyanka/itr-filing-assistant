'use client';

import { useEffect, useState } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { clientApi, ClientResponse } from '@/lib/api';

const ASSESSMENT_YEARS = ['AY2025-26', 'AY2026-27', 'AY2027-28'];

export default function ClientDetailPage() {
  const router = useRouter();
  const params = useParams();
  const clientId = Number(params.id);
  const [client, setClient] = useState<ClientResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

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

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    );
  }

  if (error || !client) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <p className="text-red-600 mb-4">{error || 'Client not found'}</p>
          <button onClick={() => router.push('/dashboard')} className="btn-primary">
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b border-gray-200">
        <div className="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
          <button
            onClick={() => router.push('/dashboard')}
            className="flex items-center gap-2 text-gray-600 hover:text-gray-900 transition-colors"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7" />
            </svg>
            Back to Dashboard
          </button>
          <h1 className="text-lg font-bold text-gray-900">Client Details</h1>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-6 py-8">
        {/* Client Info Card */}
        <div className="card mb-8">
          <div className="flex items-start justify-between mb-6">
            <div>
              <h2 className="text-2xl font-bold text-gray-900">{client.name}</h2>
              <p className="text-lg text-gray-500 font-mono mt-1">{client.pan}</p>
            </div>
            <div className="w-12 h-12 rounded-full bg-primary-100 flex items-center justify-center">
              <span className="text-primary-700 font-bold text-lg">
                {client.name.charAt(0).toUpperCase()}
              </span>
            </div>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {client.email && (
              <div>
                <p className="text-xs text-gray-400 font-medium uppercase">Email</p>
                <p className="text-sm text-gray-700 mt-1">{client.email}</p>
              </div>
            )}
            {client.mobile && (
              <div>
                <p className="text-xs text-gray-400 font-medium uppercase">Mobile</p>
                <p className="text-sm text-gray-700 mt-1">{client.mobile}</p>
              </div>
            )}
            {client.aadhaar && (
              <div>
                <p className="text-xs text-gray-400 font-medium uppercase">Aadhaar</p>
                <p className="text-sm text-gray-700 mt-1 font-mono">{client.aadhaar}</p>
              </div>
            )}
            {client.dob && (
              <div>
                <p className="text-xs text-gray-400 font-medium uppercase">Date of Birth</p>
                <p className="text-sm text-gray-700 mt-1">{client.dob}</p>
              </div>
            )}
          </div>
        </div>

        {/* Year-wise Data */}
        <h3 className="text-lg font-bold text-gray-900 mb-4">Year-wise ITR Data</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {ASSESSMENT_YEARS.map((year) => {
            const yearData = client.years.find((y) => y.year === year);
            const hasData = yearData?.status != null;

            return (
              <div key={year} className="card">
                <div className="flex items-center justify-between mb-4">
                  <h4 className="font-semibold text-gray-900">{year}</h4>
                  <span
                    className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                      hasData
                        ? 'bg-green-100 text-green-700'
                        : 'bg-red-100 text-red-700'
                    }`}
                  >
                    {hasData ? yearData!.status!.charAt(0).toUpperCase() + yearData!.status!.slice(1) : 'No Data'}
                  </span>
                </div>

                <button
                  onClick={() =>
                    router.push(`/client/${client.id}/prefill/${year}`)
                  }
                  className={`w-full text-sm py-2 rounded-lg transition-colors ${
                    hasData
                      ? 'btn-secondary'
                      : 'btn-primary'
                  }`}
                >
                  {hasData ? 'Update Prefill' : 'Upload Prefill'}
                </button>

                {hasData && (
                  <button
                    onClick={() => router.push(`/client/${client.id}/itr/${year}`)}
                    className="w-full text-sm py-2 rounded-lg transition-colors btn-primary mt-2"
                  >
                    Edit ITR-1 Form
                  </button>
                )}
              </div>
            );
          })}
        </div>
      </main>
    </div>
  );
}
