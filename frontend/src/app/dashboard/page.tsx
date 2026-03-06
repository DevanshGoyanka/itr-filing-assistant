'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useDashboardStore } from '@/store/dashboardStore';
import { clientApi, ClientRequest } from '@/lib/api';

const ASSESSMENT_YEARS = ['AY2025-26', 'AY2026-27', 'AY2027-28'];

function StatusBadge({ status }: { status: string | null }) {
  if (!status) {
    return (
      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-700">
        No Data
      </span>
    );
  }

  const colors: Record<string, string> = {
    draft: 'bg-green-100 text-green-700',
    validated: 'bg-green-100 text-green-700',
    computed: 'bg-yellow-100 text-yellow-700',
    final: 'bg-blue-100 text-blue-700',
  };

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${colors[status] || 'bg-gray-100 text-gray-700'}`}>
      {status.charAt(0).toUpperCase() + status.slice(1)}
    </span>
  );
}

function AddClientModal({
  isOpen,
  onClose,
  onSubmit,
}: {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: ClientRequest) => void;
}) {
  const [formData, setFormData] = useState<ClientRequest>({
    pan: '',
    name: '',
    email: '',
    mobile: '',
    aadhaar: '',
    dob: '',
  });
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    // PAN validation
    if (!/^[A-Z]{5}[0-9]{4}[A-Z]$/.test(formData.pan)) {
      setError('Invalid PAN format. Must be like ABCDE1234F (uppercase)');
      return;
    }

    if (!formData.name.trim()) {
      setError('Name is required');
      return;
    }

    // Aadhaar validation
    if (formData.aadhaar && !/^[0-9]{12}$/.test(formData.aadhaar)) {
      setError('Aadhaar must be exactly 12 numeric digits');
      return;
    }

    onSubmit(formData);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-white rounded-xl shadow-xl max-w-lg w-full mx-4 p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-bold">Add New Client</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">PAN *</label>
              <input
                className="input-field"
                placeholder="ABCDE1234F"
                value={formData.pan}
                onChange={(e) => setFormData({ ...formData, pan: e.target.value.toUpperCase() })}
                maxLength={10}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Name *</label>
              <input
                className="input-field"
                placeholder="Full Name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                maxLength={125}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
              <input
                type="email"
                className="input-field"
                placeholder="client@example.com"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Mobile</label>
              <input
                className="input-field"
                placeholder="+91XXXXXXXXXX"
                value={formData.mobile}
                onChange={(e) => setFormData({ ...formData, mobile: e.target.value })}
                maxLength={15}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Aadhaar</label>
              <input
                className="input-field"
                placeholder="12 digit number"
                value={formData.aadhaar}
                onChange={(e) => setFormData({ ...formData, aadhaar: e.target.value.replace(/\D/g, '') })}
                maxLength={12}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Date of Birth</label>
              <input
                type="date"
                className="input-field"
                value={formData.dob}
                onChange={(e) => setFormData({ ...formData, dob: e.target.value })}
              />
            </div>
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-3">
              <p className="text-red-600 text-sm">{error}</p>
            </div>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary">
              Cancel
            </button>
            <button type="submit" className="btn-primary">
              Create Client
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const router = useRouter();
  const { user, clients, selectedYear, fetchClients, setSelectedYear, logout, loading } =
    useDashboardStore();
  const [showAddClient, setShowAddClient] = useState(false);
  const [createError, setCreateError] = useState('');

  useEffect(() => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('token');
      if (!token) {
        router.push('/login');
        return;
      }
    }
    fetchClients();
  }, [fetchClients, router]);

  const handleCreateClient = async (data: ClientRequest) => {
    try {
      setCreateError('');
      await clientApi.create(data);
      setShowAddClient(false);
      fetchClients();
    } catch (err: any) {
      setCreateError(err.response?.data?.message || 'Failed to create client');
    }
  };

  const handleLogout = () => {
    logout();
    router.push('/login');
  };

  const getYearStatus = (client: { years: { year: string; status: string | null }[] }) => {
    const yearData = client.years.find((y) => y.year === selectedYear);
    return yearData?.status || null;
  };

  const userEmail = user?.email || (typeof window !== 'undefined' ? localStorage.getItem('email') : '') || '';

  return (
    <div className="min-h-screen flex">
      {/* Sidebar */}
      <aside className="w-72 bg-white border-r border-gray-200 flex flex-col">
        <div className="p-6 border-b border-gray-100">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center">
              <svg className="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
            <div>
              <h1 className="font-bold text-gray-900">ITR-1 Assistant</h1>
              <p className="text-xs text-gray-500 truncate">{userEmail}</p>
            </div>
          </div>
        </div>

        <div className="p-6 flex-1">
          <div className="mb-6">
            <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">Clients</p>
            <p className="text-3xl font-bold text-gray-900">{clients.length}</p>
          </div>

          <div className="mb-6">
            <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">
              Assessment Year
            </p>
            <div className="space-y-1">
              {ASSESSMENT_YEARS.map((year) => (
                <button
                  key={year}
                  onClick={() => setSelectedYear(year)}
                  className={`w-full text-left px-3 py-2 rounded-lg text-sm transition-colors ${
                    selectedYear === year
                      ? 'bg-primary-50 text-primary-700 font-medium'
                      : 'text-gray-600 hover:bg-gray-50'
                  }`}
                >
                  {year}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="p-6 border-t border-gray-100">
          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-2 text-sm text-gray-600 hover:text-red-600 transition-colors"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            Sign Out
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 p-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">Dashboard</h2>
            <p className="text-gray-500 mt-1">Manage your clients and ITR filings</p>
          </div>
          <button onClick={() => setShowAddClient(true)} className="btn-primary flex items-center gap-2">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
            </svg>
            Add New Client
          </button>
        </div>

        {createError && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-3 mb-6">
            <p className="text-red-600 text-sm">{createError}</p>
          </div>
        )}

        {loading ? (
          <div className="flex items-center justify-center py-20">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
          </div>
        ) : clients.length === 0 ? (
          <div className="text-center py-20">
            <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gray-100 mb-4">
              <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                  d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </div>
            <h3 className="text-lg font-medium text-gray-900">No clients yet</h3>
            <p className="text-gray-500 mt-1">Add your first client to get started</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {clients.map((client) => (
              <div key={client.id} className="card hover:shadow-md transition-shadow">
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <h3 className="font-semibold text-gray-900">{client.name}</h3>
                    <p className="text-sm text-gray-500 font-mono mt-0.5">{client.pan}</p>
                  </div>
                  <StatusBadge status={getYearStatus(client)} />
                </div>

                {client.email && (
                  <p className="text-sm text-gray-500 mb-1">📧 {client.email}</p>
                )}
                {client.mobile && (
                  <p className="text-sm text-gray-500 mb-3">📱 {client.mobile}</p>
                )}

                <div className="flex flex-wrap gap-1 mb-4">
                  {ASSESSMENT_YEARS.map((year) => {
                    const yearData = client.years.find((y) => y.year === year);
                    return (
                      <span
                        key={year}
                        className={`text-xs px-2 py-1 rounded ${
                          yearData?.status
                            ? 'bg-green-50 text-green-600'
                            : 'bg-gray-50 text-gray-400'
                        }`}
                      >
                        {year}: {yearData?.status || 'No data'}
                      </span>
                    );
                  })}
                </div>

                <div className="flex gap-2 pt-3 border-t border-gray-100">
                  <button
                    onClick={() => router.push(`/client/${client.id}`)}
                    className="flex-1 text-sm btn-secondary py-2"
                  >
                    View
                  </button>
                  <button
                    onClick={() =>
                      router.push(`/client/${client.id}/prefill/${selectedYear}`)
                    }
                    className="flex-1 text-sm btn-primary py-2"
                  >
                    Add Prefill
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        <AddClientModal
          isOpen={showAddClient}
          onClose={() => setShowAddClient(false)}
          onSubmit={handleCreateClient}
        />
      </main>
    </div>
  );
}
