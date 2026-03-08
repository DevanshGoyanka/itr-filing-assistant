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
    <div style={{ minHeight: '100vh', background: '#f0f4f8', fontFamily: "'Segoe UI',system-ui,sans-serif" }}>
      <header style={{ background: '#0d2137', borderBottom: '1px solid #1a3550', position: 'sticky', top: 0, zIndex: 10 }}>
        <div style={{ maxWidth: 1100, margin: '0 auto', padding: '0 24px', height: 46, display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <div style={{ width: 28, height: 28, borderRadius: 7, background: 'linear-gradient(135deg,#1976d2,#42a5f5)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 14, fontWeight: 900, color: '#fff' }}>₹</div>
            <div style={{ fontSize: 13.5, fontWeight: 800, color: '#e2eaf6' }}>MyTaxERP</div>
          </div>
          <span style={{ color: '#1a3550', fontSize: 16, margin: '0 4px' }}>›</span>
          <button
            onClick={() => router.push('/dashboard')}
            style={{ background: 'none', border: 'none', color: '#7a9ab8', cursor: 'pointer', fontSize: 12.5, padding: 0 }}
          >
            Dashboard
          </button>
          <span style={{ color: '#1a3550', fontSize: 16, margin: '0 4px' }}>›</span>
          <span style={{ fontSize: 12.5, color: '#e2eaf6', fontWeight: 600 }}>Client Details</span>
          <div style={{ flex: 1 }} />
          <button
            onClick={() => router.push('/dashboard')}
            style={{ background: 'none', border: '1px solid #1e3a5f', color: '#7a9ab8', cursor: 'pointer', fontSize: 11.5, padding: '4px 12px', borderRadius: 5 }}
          >
            ← Back
          </button>
        </div>
      </header>

      <main style={{ maxWidth: 1100, margin: '0 auto', padding: '24px' }}>
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
