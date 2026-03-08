'use client';

import { useEffect, useState, useMemo, FormEvent, ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import { useDashboardStore } from '@/store/dashboardStore';
import { clientApi, ClientRequest } from '@/lib/api';

const ASSESSMENT_YEARS = ['AY2025-26', 'AY2026-27', 'AY2027-28'];

const NAV_ITEMS = [
  { id: 'dashboard', label: 'Dashboard',       icon: '📊' },
  { id: 'clients',   label: 'Client Master',   icon: '👥' },
  { id: 'itr',       label: 'ITR Computation', icon: '🧾' },
  { id: 'office',    label: 'Office Mgmt',     icon: '📋' },
  { id: 'billing',   label: 'Billing',         icon: '💼' },
  { id: 'reports',   label: 'Reports',         icon: '📈' },
];

const STATUS_STYLES: Record<string, { bg: string; color: string; dot: string }> = {
  draft:     { bg: '#dcfce7', color: '#15803d', dot: '#16a34a' },
  validated: { bg: '#dcfce7', color: '#15803d', dot: '#16a34a' },
  computed:  { bg: '#fef9c3', color: '#92400e', dot: '#d97706' },
  final:     { bg: '#dbeafe', color: '#1e40af', dot: '#3b82f6' },
};

function StatusBadge({ status }: { status: string | null }) {
  const s = STATUS_STYLES[status ?? ''] ?? { bg: '#fee2e2', color: '#991b1b', dot: '#dc2626' };
  const label = status ? status.charAt(0).toUpperCase() + status.slice(1) : 'No Data';
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, fontSize: 10.5, fontWeight: 600, background: s.bg, color: s.color, padding: '2px 8px', borderRadius: 20 }}>
      <span style={{ width: 6, height: 6, borderRadius: '50%', background: s.dot, display: 'inline-block' }} />
      {label}
    </span>
  );
}

function Metric({ label, value, sub, color = '#1565c0', icon }: {
  label: string; value: string | number; sub?: string; color?: string; icon?: string;
}) {
  return (
    <div style={{ background: '#fff', borderRadius: 10, border: '1px solid #e8edf5', padding: '14px 16px', flex: 1, minWidth: 0 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div style={{ minWidth: 0, overflow: 'hidden' }}>
          <div style={{ fontSize: 10.5, fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 6 }}>{label}</div>
          <div style={{ fontSize: 22, fontWeight: 800, color, fontFamily: "'Courier New',monospace", lineHeight: 1 }}>{value}</div>
          {sub && <div style={{ fontSize: 11, color: '#64748b', marginTop: 4 }}>{sub}</div>}
        </div>
        {icon && <div style={{ fontSize: 24, opacity: 0.18, flexShrink: 0 }}>{icon}</div>}
      </div>
    </div>
  );
}

function AddClientModal({
  isOpen,
  onClose,
  onSubmit,
  editClient,
}: {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: ClientRequest) => void;
  editClient?: ClientRequest & { id?: number };
}) {
  const [formData, setFormData] = useState<ClientRequest>({ pan: '', name: '', email: '', mobile: '', aadhaar: '', dob: '' });
  const [error, setError] = useState('');

  useEffect(() => {
    if (editClient) {
      setFormData({ pan: editClient.pan || '', name: editClient.name || '', email: editClient.email || '', mobile: editClient.mobile || '', aadhaar: editClient.aadhaar || '', dob: editClient.dob || '' });
    } else {
      setFormData({ pan: '', name: '', email: '', mobile: '', aadhaar: '', dob: '' });
    }
    setError('');
  }, [editClient, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!/^[A-Z]{5}[0-9]{4}[A-Z]$/.test(formData.pan)) {
      setError('Invalid PAN format. Must be like ABCDE1234F (uppercase)');
      return;
    }
    if (!formData.name.trim()) { setError('Name is required'); return; }
    if (formData.aadhaar && !/^[0-9]{12}$/.test(formData.aadhaar)) {
      setError('Aadhaar must be exactly 12 numeric digits');
      return;
    }
    onSubmit(formData);
  };

  const F = ({ label, children }: { label: string; children: ReactNode }) => (
    <div>
      <div style={{ fontSize: 12, fontWeight: 600, color: '#374151', marginBottom: 4 }}>{label}</div>
      {children}
    </div>
  );

  const iStyle = { width: '100%', border: '1.5px solid #e2e8f0', borderRadius: 6, padding: '7px 10px', fontSize: 13, outline: 'none' as const };

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.55)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ background: '#fff', borderRadius: 12, width: 520, boxShadow: '0 20px 60px rgba(0,0,0,0.3)', overflow: 'hidden' }}>
        <div style={{ background: '#0f172a', padding: '14px 18px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontSize: 13.5, fontWeight: 700, color: '#e2eaf6' }}>{editClient ? '✏️ Edit Client' : '➕ Add New Client'}</span>
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: '#5a7a9a', cursor: 'pointer', fontSize: 18, lineHeight: 1 }}>✕</button>
        </div>
        <form onSubmit={handleSubmit} style={{ padding: '18px 20px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px 16px', marginBottom: 14 }}>
            <F label="PAN *"><input style={iStyle} placeholder="ABCDE1234F" value={formData.pan} maxLength={10} onChange={(e) => setFormData((p) => ({ ...p, pan: e.target.value.toUpperCase() }))} /></F>
            <F label="Name *"><input style={iStyle} placeholder="Full Name" value={formData.name} maxLength={125} onChange={(e) => setFormData((p) => ({ ...p, name: e.target.value }))} /></F>
            <F label="Email"><input style={iStyle} type="email" placeholder="client@example.com" value={formData.email} onChange={(e) => setFormData((p) => ({ ...p, email: e.target.value }))} /></F>
            <F label="Mobile"><input style={iStyle} placeholder="+91XXXXXXXXXX" value={formData.mobile} maxLength={15} onChange={(e) => setFormData((p) => ({ ...p, mobile: e.target.value }))} /></F>
            <F label="Aadhaar"><input style={iStyle} placeholder="12 digit number" value={formData.aadhaar} maxLength={12} onChange={(e) => setFormData((p) => ({ ...p, aadhaar: e.target.value.replace(/\D/g, '') }))} /></F>
            <F label="Date of Birth"><input style={iStyle} type="date" value={formData.dob} onChange={(e) => setFormData((p) => ({ ...p, dob: e.target.value }))} /></F>
          </div>
          {error && (
            <div style={{ background: '#fee2e2', border: '1px solid #fca5a5', borderRadius: 6, padding: '8px 12px', fontSize: 12, color: '#991b1b', marginBottom: 12 }}>{error}</div>
          )}
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
            <button type="button" onClick={onClose} style={{ padding: '8px 16px', background: '#f1f5f9', border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 12.5, fontWeight: 600, color: '#475569' }}>Cancel</button>
            <button type="submit" style={{ padding: '8px 16px', background: '#1565c0', border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 12.5, fontWeight: 700, color: '#fff' }}>
              {editClient ? 'Update Client' : 'Create Client'}
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
  const [editingClient, setEditingClient] = useState<(ClientRequest & { id?: number }) | undefined>();
  const [createError, setCreateError] = useState('');
  const [userEmail, setUserEmail] = useState('');
  const [search, setSearch] = useState('');
  const [filterStatus, setFilterStatus] = useState('All');
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mounted, setMounted] = useState(false);
  const [comingSoon, setComingSoon] = useState('');

  useEffect(() => {
    setMounted(true);
    const token = localStorage.getItem('token');
    if (!token) { router.push('/login'); return; }
    setUserEmail(user?.email || localStorage.getItem('email') || '');
    fetchClients();
  }, [fetchClients, router, user?.email]);

  const getYearStatus = (client: { years: { year: string; status: string | null }[] }) =>
    client.years.find((y) => y.year === selectedYear)?.status ?? null;

  const filteredClients = useMemo(() => {
    const lc = search.toLowerCase();
    return clients.filter((c) => {
      const status = getYearStatus(c as any);
      const matchSearch = !search || c.name.toLowerCase().includes(lc) || c.pan.toLowerCase().includes(lc);
      const matchFilter =
        filterStatus === 'All' ||
        (filterStatus === 'No Data' ? !status : status === filterStatus.toLowerCase());
      return matchSearch && matchFilter;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clients, search, filterStatus, selectedYear]);

  const handleCreateClient = async (data: ClientRequest) => {
    try {
      setCreateError('');
      const cleanedData = {
        ...data,
        email: data.email || undefined,
        mobile: data.mobile || undefined,
        aadhaar: data.aadhaar || undefined,
        dob: data.dob || undefined,
      };
      if (editingClient?.id) {
        await clientApi.update(editingClient.id, cleanedData);
      } else {
        await clientApi.create(cleanedData);
      }
      setShowAddClient(false);
      setEditingClient(undefined);
      fetchClients();
    } catch (err: any) {
      setCreateError(err.response?.data?.message || err.message || 'Failed to save client');
    }
  };

  const handleEditClient = (client: any) => {
    setEditingClient({ id: client.id, pan: client.pan, name: client.name, email: client.email || '', mobile: client.mobile || '', aadhaar: client.aadhaar || '', dob: client.dob || '' });
    setShowAddClient(true);
  };

  const handleLogout = () => { logout(); router.push('/login'); };

  const filed  = clients.filter((c) => getYearStatus(c as any) != null).length;
  const noData = clients.filter((c) => getYearStatus(c as any) == null).length;

  return (
    <div style={{ height: '100vh', display: 'flex', flexDirection: 'column', fontFamily: "'Segoe UI',system-ui,sans-serif", background: '#f0f4f8', overflow: 'hidden' }}>
      <style>{`*{box-sizing:border-box}::-webkit-scrollbar{width:5px;height:5px}::-webkit-scrollbar-thumb{background:#c1cdd8;border-radius:3px}@keyframes spin{to{transform:rotate(360deg)}}`}</style>

      {/* TOP BAR */}
      <div style={{ height: 46, background: '#0d2137', display: 'flex', alignItems: 'center', padding: '0 14px', gap: 10, flexShrink: 0, borderBottom: '1px solid #1a3550' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div style={{ width: 28, height: 28, borderRadius: 7, background: 'linear-gradient(135deg,#1976d2,#42a5f5)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 14, fontWeight: 900, color: '#fff' }}>₹</div>
          <div>
            <div style={{ fontSize: 14, fontWeight: 800, color: '#e2eaf6', lineHeight: 1 }}>MyTaxERP</div>
            <div style={{ fontSize: 8.5, color: '#3a6a8a', letterSpacing: '0.05em' }}>INCOME TAX FILING ASSISTANT</div>
          </div>
        </div>
        <div style={{ flex: 1 }} />
        <span style={{ fontSize: 11, color: '#3a6a8a', paddingRight: 12, borderRight: '1px solid #1a3550' }}>
          {clients.length} Clients · {filed} with data
        </span>
        <div style={{ display: 'flex', alignItems: 'center', gap: 7, background: '#0a1829', border: '1px solid #1e3a5f', borderRadius: 7, padding: '5px 10px' }}>
          <div style={{ width: 22, height: 22, borderRadius: '50%', background: '#1565c0', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 10, fontWeight: 700, color: '#fff' }}>
            {(userEmail.charAt(0) || 'U').toUpperCase()}
          </div>
          <div>
            <div style={{ fontSize: 11.5, fontWeight: 700, color: '#e2eaf6', maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{userEmail || 'User'}</div>
            <div style={{ fontSize: 9, color: '#3a6a8a' }}>{selectedYear}</div>
          </div>
        </div>
        <button onClick={handleLogout} style={{ background: 'none', border: '1px solid #1e3a5f', color: '#5a7a9a', borderRadius: 6, padding: '5px 10px', cursor: 'pointer', fontSize: 11.5, fontWeight: 600 }}>
          Sign Out
        </button>
      </div>

      {/* BODY */}
      <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        {/* Sidebar */}
        <div style={{ width: sidebarCollapsed ? 46 : 178, background: '#0d2137', display: 'flex', flexDirection: 'column', transition: 'width 0.2s', overflow: 'hidden', flexShrink: 0, borderRight: '1px solid #1a3550' }}>
          <div style={{ padding: '8px 6px', flex: 1, overflowY: 'auto', overflowX: 'hidden' }}>
            {NAV_ITEMS.map((item) => {
              const active = item.id === 'dashboard';
              return (
                <button
                  key={item.id}
                  title={sidebarCollapsed ? item.label : ''}
                  onClick={() => { if (item.id !== 'dashboard') { setComingSoon(item.label + ' — Development in process, coming soon'); setTimeout(() => setComingSoon(''), 3000); } }}
                  style={{ width: '100%', display: 'flex', alignItems: 'center', gap: 8, padding: sidebarCollapsed ? '9px 0' : '8px 10px', justifyContent: sidebarCollapsed ? 'center' : 'flex-start', background: active ? '#1e40af' : 'transparent', border: 'none', borderRadius: 6, cursor: 'pointer', color: active ? '#fff' : '#7a9ab8', marginBottom: 2, transition: 'all 0.1s' }}
                  onMouseEnter={(e) => { if (!active) e.currentTarget.style.background = '#1a3550'; }}
                  onMouseLeave={(e) => { if (!active) e.currentTarget.style.background = 'transparent'; }}
                >
                  <span style={{ fontSize: 15, flexShrink: 0 }}>{item.icon}</span>
                  {!sidebarCollapsed && <span style={{ fontSize: 12, fontWeight: active ? 700 : 400, whiteSpace: 'nowrap' }}>{item.label}</span>}
                </button>
              );
            })}
          </div>
          <div style={{ padding: '8px 6px', borderTop: '1px solid #1a3550', flexShrink: 0 }}>
            {!sidebarCollapsed && clients.length > 0 && (
              <div style={{ padding: '6px 10px', marginBottom: 6 }}>
                <div style={{ fontSize: 9, color: '#3a6a8a', marginBottom: 3 }}>FILING PROGRESS</div>
                <div style={{ height: 4, background: '#0a1829', borderRadius: 2, marginBottom: 3 }}>
                  <div style={{ width: `${Math.round(filed / clients.length * 100)}%`, height: '100%', background: '#42a5f5', borderRadius: 2 }} />
                </div>
                <div style={{ fontSize: 9.5, color: '#4a7a9a' }}>{filed}/{clients.length} with data</div>
              </div>
            )}
            <button onClick={() => setSidebarCollapsed((p) => !p)} style={{ width: '100%', background: 'none', border: 'none', color: '#3a6a8a', cursor: 'pointer', fontSize: 18, padding: '3px 0', textAlign: 'center' }}>
              {sidebarCollapsed ? '›' : '‹'}
            </button>
          </div>
        </div>

        {/* Main Content */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
          {/* Breadcrumb + AY switcher */}
          <div style={{ background: '#fff', borderBottom: '1px solid #e8edf5', padding: '5px 18px', flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <span style={{ fontSize: 10.5, color: '#94a3b8' }}>MyTaxERP</span>
              <span style={{ fontSize: 10.5, color: '#94a3b8' }}>›</span>
              <span style={{ fontSize: 12.5, fontWeight: 700, color: '#0f172a' }}>📊 Dashboard</span>
            </div>
            <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
              <span style={{ fontSize: 11, color: '#64748b' }}>Assessment Year:</span>
              {ASSESSMENT_YEARS.map((year) => (
                <button key={year} onClick={() => setSelectedYear(year)} style={{ background: selectedYear === year ? '#1565c0' : '#f1f5f9', color: selectedYear === year ? '#fff' : '#475569', border: 'none', borderRadius: 5, padding: '4px 10px', fontSize: 11.5, cursor: 'pointer', fontWeight: selectedYear === year ? 700 : 400 }}>
                  {year}
                </button>
              ))}
            </div>
          </div>

          {/* Scrollable area */}
          <div style={{ flex: 1, overflowY: 'auto', padding: '16px 20px' }}>
            {/* Metrics */}
            <div style={{ display: 'flex', gap: 12, marginBottom: 18 }}>
              <Metric label="Total Clients" value={clients.length} sub={selectedYear} icon="👥" />
              <Metric label="With ITR Data" value={filed} sub={clients.length > 0 ? `${Math.round(filed / clients.length * 100)}% have data` : '—'} color="#059669" icon="✅" />
              <Metric label="No Data Yet" value={noData} sub="Prefill not uploaded" color="#d97706" icon="⏳" />
              <Metric label="Active Year" value={selectedYear.replace('AY', 'AY ')} sub="Click year above to switch" color="#7c3aed" icon="📅" />
            </div>

            {createError && (
              <div style={{ background: '#fee2e2', border: '1px solid #fca5a5', borderRadius: 8, padding: '10px 14px', fontSize: 12.5, color: '#991b1b', marginBottom: 14 }}>{createError}</div>
            )}

            {/* Client table */}
            <div style={{ background: '#fff', borderRadius: 10, border: '1px solid #e8edf5', overflow: 'hidden' }}>
              {/* Toolbar */}
              <div style={{ padding: '10px 16px', borderBottom: '1px solid #f1f5f9', display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
                <div style={{ position: 'relative' }}>
                  <span style={{ position: 'absolute', left: 9, top: '50%', transform: 'translateY(-50%)', fontSize: 12, color: '#94a3b8', pointerEvents: 'none' }}>🔍</span>
                  <input
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    placeholder="Search name or PAN…"
                    style={{ width: 240, border: '1.5px solid #e2e8f0', borderRadius: 6, padding: '6px 10px 6px 28px', fontSize: 12.5, outline: 'none' }}
                    onFocus={(e) => (e.target.style.borderColor = '#3b82f6')}
                    onBlur={(e) => (e.target.style.borderColor = '#e2e8f0')}
                  />
                </div>
                {['All', 'No Data', 'Computed', 'Final', 'Draft'].map((f) => (
                  <button key={f} onClick={() => setFilterStatus(f)} style={{ background: filterStatus === f ? '#1565c0' : '#f1f5f9', color: filterStatus === f ? '#fff' : '#475569', border: 'none', borderRadius: 5, padding: '5px 12px', fontSize: 11.5, cursor: 'pointer', fontWeight: 600 }}>
                    {f}
                  </button>
                ))}
                <div style={{ flex: 1 }} />
                <button
                  onClick={() => { setEditingClient(undefined); setShowAddClient(true); }}
                  style={{ background: '#0f172a', border: 'none', color: '#e2eaf6', borderRadius: 6, padding: '6px 14px', fontSize: 12, cursor: 'pointer', fontWeight: 700 }}
                >
                  + Add Client
                </button>
              </div>

              {/* Table */}
              {loading ? (
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: 200, gap: 10 }}>
                  <div style={{ width: 24, height: 24, borderRadius: '50%', border: '3px solid #e2e8f0', borderTopColor: '#1565c0', animation: 'spin 0.7s linear infinite' }} />
                  <span style={{ fontSize: 13, color: '#94a3b8' }}>Loading clients…</span>
                </div>
              ) : filteredClients.length === 0 ? (
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: 200 }}>
                  <div style={{ fontSize: 36, marginBottom: 10 }}>👥</div>
                  <div style={{ fontSize: 14, fontWeight: 600, color: '#64748b' }}>
                    {clients.length === 0 ? 'No clients yet — add your first client' : 'No clients match the filter'}
                  </div>
                </div>
              ) : (
                <div style={{ overflowX: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                    <thead>
                      <tr style={{ background: '#f8fafc' }}>
                        {['Client Name', 'PAN', 'Contact', `${selectedYear} Status`, 'Actions'].map((h) => (
                          <th key={h} style={{ padding: '8px 14px', textAlign: 'left', fontSize: 10, fontWeight: 700, color: '#94a3b8', letterSpacing: '0.05em', textTransform: 'uppercase', borderBottom: '1.5px solid #e8edf5', whiteSpace: 'nowrap' }}>{h}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {filteredClients.map((client) => {
                        const status = getYearStatus(client as any);
                        return (
                          <tr
                            key={client.id}
                            style={{ borderBottom: '1px solid #f1f5f9', cursor: 'pointer' }}
                            onMouseEnter={(e) => (e.currentTarget.style.background = '#f8fafc')}
                            onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
                            onClick={() => router.push(`/client/${client.id}`)}
                          >
                            <td style={{ padding: '10px 14px' }}>
                              <div style={{ fontSize: 12.5, fontWeight: 600, color: '#0f172a' }}>{client.name}</div>
                            </td>
                            <td style={{ padding: '10px 14px', fontFamily: 'monospace', fontSize: 12, fontWeight: 600, color: '#374151' }}>{client.pan}</td>
                            <td style={{ padding: '10px 14px' }}>
                              {client.email && <div style={{ fontSize: 11.5, color: '#64748b' }}>{client.email}</div>}
                              {client.mobile && <div style={{ fontSize: 11, color: '#94a3b8' }}>{client.mobile}</div>}
                              {!client.email && !client.mobile && <span style={{ fontSize: 11, color: '#cbd5e1' }}>—</span>}
                            </td>
                            <td style={{ padding: '10px 14px' }}>
                              <StatusBadge status={status} />
                            </td>
                            <td style={{ padding: '10px 14px' }}>
                              <div style={{ display: 'flex', gap: 6 }} onClick={(e) => e.stopPropagation()}>
                                <button
                                  onClick={() => handleEditClient(client)}
                                  style={{ background: '#f1f5f9', border: 'none', color: '#374151', borderRadius: 5, padding: '4px 10px', fontSize: 11, cursor: 'pointer' }}
                                  title="Edit client"
                                >
                                  ✏️ Edit
                                </button>
                                <button
                                  onClick={() => router.push(`/client/${client.id}/prefill/${selectedYear}`)}
                                  style={{ background: '#eff6ff', border: 'none', color: '#1d4ed8', borderRadius: 5, padding: '4px 10px', fontSize: 11, cursor: 'pointer', fontWeight: 600 }}
                                >
                                  Prefill
                                </button>
                                {status && (
                                  <button
                                    onClick={() => router.push(`/client/${client.id}/itr/${selectedYear}`)}
                                    style={{ background: '#1565c0', border: 'none', color: '#fff', borderRadius: 5, padding: '4px 10px', fontSize: 11, cursor: 'pointer', fontWeight: 600 }}
                                  >
                                    Open ITR →
                                  </button>
                                )}
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* STATUS BAR */}
      <div style={{ height: 22, background: '#0d2137', display: 'flex', alignItems: 'center', padding: '0 14px', flexShrink: 0 }}>
        {[`${clients.length} Clients`, `${filed} With Data`, `${noData} No Data`, `${selectedYear} Active`].map((item, i, arr) => (
          <span key={i} style={{ fontSize: 10, color: '#3a6a8a', paddingRight: 12, borderRight: i < arr.length - 1 ? '1px solid #1a3550' : 'none', marginRight: 12, whiteSpace: 'nowrap' }}>{item}</span>
        ))}
        <div style={{ flex: 1 }} />
        <span style={{ fontSize: 10, color: '#2a4565' }} suppressHydrationWarning>{mounted ? new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : ''}</span>
      </div>

      {comingSoon && (
        <div style={{ position: 'fixed', top: 60, left: '50%', transform: 'translateX(-50%)', background: '#1e293b', color: '#e2e8f0', padding: '10px 24px', borderRadius: 8, fontSize: 13, fontWeight: 600, zIndex: 9999, boxShadow: '0 4px 20px rgba(0,0,0,0.3)', border: '1px solid #334155' }}>
          🚧 {comingSoon}
        </div>
      )}

      <AddClientModal
        isOpen={showAddClient}
        onClose={() => { setShowAddClient(false); setEditingClient(undefined); }}
        onSubmit={handleCreateClient}
        editClient={editingClient}
      />
    </div>
  );
}
