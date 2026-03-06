'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { itrApi, Itr1FormData } from '@/lib/api';

type Tab = 'general' | 'salary' | 'hp' | 'os' | 'deductions' | 'taxes' | 'computation';

const TABS: { key: Tab; label: string }[] = [
  { key: 'general', label: 'General Info' },
  { key: 'salary', label: 'Salary' },
  { key: 'hp', label: 'House Property' },
  { key: 'os', label: 'Other Sources' },
  { key: 'deductions', label: 'Deductions' },
  { key: 'taxes', label: 'Taxes Paid' },
  { key: 'computation', label: 'Tax Computation' },
];

const fmt = (n: number) =>
  n != null ? '₹' + n.toLocaleString('en-IN', { maximumFractionDigits: 0 }) : '₹0';

export default function ItrFormPage() {
  const router = useRouter();
  const params = useParams();
  const clientId = Number(params.id);
  const year = params.year as string;

  const [form, setForm] = useState<Itr1FormData | null>(null);
  const [activeTab, setActiveTab] = useState<Tab>('general');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [computing, setComputing] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  useEffect(() => {
    if (typeof window !== 'undefined' && !localStorage.getItem('token')) {
      router.push('/login');
      return;
    }
    loadForm();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clientId, year]);

  const loadForm = async () => {
    try {
      const res = await itrApi.getFormData(clientId, year);
      setForm(res.data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load form data');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    if (!form) return;
    setSaving(true);
    setError('');
    setSuccessMsg('');
    try {
      const res = await itrApi.saveFormData(clientId, year, form);
      setForm(res.data);
      setSuccessMsg('Form saved successfully');
      setTimeout(() => setSuccessMsg(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save');
    } finally {
      setSaving(false);
    }
  };

  const handleCompute = async () => {
    if (!form) return;
    setComputing(true);
    setError('');
    setSuccessMsg('');
    try {
      const res = await itrApi.computeForm(clientId, year, form);
      setForm(res.data);
      setActiveTab('computation');
      setSuccessMsg('Tax computed successfully');
      setTimeout(() => setSuccessMsg(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Computation failed');
    } finally {
      setComputing(false);
    }
  };

  const handleDownload = async () => {
    try {
      const res = await itrApi.downloadJson(clientId, year);
      const blob = new Blob([JSON.stringify(res.data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `ITR1_${year}.json`;
      a.click();
      URL.revokeObjectURL(url);
      setSuccessMsg('ITR-1 JSON downloaded');
      setTimeout(() => setSuccessMsg(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Download failed. Please compute first.');
    }
  };

  // Generic updater helpers
  const updatePartA = useCallback(
    (field: string, value: any) => {
      if (!form) return;
      setForm({ ...form, partA: { ...form.partA, [field]: value } });
    },
    [form]
  );

  const updateSalary = useCallback(
    (field: string, value: number) => {
      if (!form) return;
      setForm({ ...form, scheduleSalary: { ...form.scheduleSalary, [field]: value } });
    },
    [form]
  );

  const updateHP = useCallback(
    (field: string, value: any) => {
      if (!form) return;
      setForm({ ...form, scheduleHP: { ...form.scheduleHP, [field]: value } });
    },
    [form]
  );

  const updateOS = useCallback(
    (field: string, value: number) => {
      if (!form) return;
      setForm({ ...form, scheduleOS: { ...form.scheduleOS, [field]: value } });
    },
    [form]
  );

  const updateDed = useCallback(
    (field: string, value: number) => {
      if (!form) return;
      setForm({ ...form, deductionsVIA: { ...form.deductionsVIA, [field]: value } });
    },
    [form]
  );

  const updateTax = useCallback(
    (field: string, value: number) => {
      if (!form) return;
      setForm({ ...form, taxesPaid: { ...form.taxesPaid, [field]: value } });
    },
    [form]
  );

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    );
  }

  if (!form) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <p className="text-red-600 mb-4">{error || 'No form data available'}</p>
          <button onClick={() => router.back()} className="btn-primary">Go Back</button>
        </div>
      </div>
    );
  }

  const isNew = form.partA?.newTaxRegime;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button onClick={() => router.back()} className="text-gray-500 hover:text-gray-900">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <div>
              <h1 className="text-lg font-bold text-gray-900">ITR-1 Form — {year}</h1>
              <p className="text-xs text-gray-500">
                {form.partA?.assesseeName} | {form.partA?.pan} |{' '}
                <span className={isNew ? 'text-blue-600 font-semibold' : 'text-orange-600 font-semibold'}>
                  {isNew ? 'New Regime (115BAC)' : 'Old Regime'}
                </span>
              </p>
            </div>
          </div>
          <div className="flex gap-2">
            <button onClick={handleSave} disabled={saving} className="btn-secondary text-sm px-4 py-2">
              {saving ? 'Saving...' : 'Save'}
            </button>
            <button onClick={handleCompute} disabled={computing} className="btn-primary text-sm px-4 py-2">
              {computing ? 'Computing...' : 'Compute Tax'}
            </button>
            <button onClick={handleDownload} className="text-sm px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors">
              Download JSON
            </button>
          </div>
        </div>
      </header>

      {/* Alerts */}
      {error && (
        <div className="max-w-7xl mx-auto px-4 mt-3">
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm flex justify-between">
            {error}
            <button onClick={() => setError('')} className="text-red-500 hover:text-red-700">✕</button>
          </div>
        </div>
      )}
      {successMsg && (
        <div className="max-w-7xl mx-auto px-4 mt-3">
          <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg text-sm">
            {successMsg}
          </div>
        </div>
      )}

      {/* Validation Errors */}
      {form.validationErrors?.length > 0 && (
        <div className="max-w-7xl mx-auto px-4 mt-3">
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <h4 className="text-sm font-semibold text-red-800 mb-2">Validation Errors ({form.validationErrors.length})</h4>
            <ul className="text-sm text-red-700 space-y-1">
              {form.validationErrors.map((e, i) => <li key={i}>• {e}</li>)}
            </ul>
          </div>
        </div>
      )}
      {form.validationWarnings?.length > 0 && (
        <div className="max-w-7xl mx-auto px-4 mt-3">
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <h4 className="text-sm font-semibold text-yellow-800 mb-2">Suggestions</h4>
            <ul className="text-sm text-yellow-700 space-y-1">
              {form.validationWarnings.map((w, i) => <li key={i}>• {w}</li>)}
            </ul>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div className="max-w-7xl mx-auto px-4 mt-4">
        <div className="flex gap-1 overflow-x-auto border-b border-gray-200 mb-6">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`px-4 py-2.5 text-sm font-medium whitespace-nowrap transition-colors border-b-2 -mb-px ${
                activeTab === tab.key
                  ? 'border-primary-600 text-primary-700'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Tab Content */}
        <div className="pb-12">
          {activeTab === 'general' && <GeneralInfoTab form={form} updatePartA={updatePartA} />}
          {activeTab === 'salary' && <SalaryTab form={form} updateSalary={updateSalary} />}
          {activeTab === 'hp' && <HousePropertyTab form={form} updateHP={updateHP} />}
          {activeTab === 'os' && <OtherSourcesTab form={form} updateOS={updateOS} />}
          {activeTab === 'deductions' && <DeductionsTab form={form} updateDed={updateDed} />}
          {activeTab === 'taxes' && <TaxesPaidTab form={form} updateTax={updateTax} />}
          {activeTab === 'computation' && <ComputationTab form={form} />}
        </div>
      </div>
    </div>
  );
}

/* ════════════════════════════════════════════════════════════════════
   REUSABLE — Number Input Row
   ════════════════════════════════════════════════════════════════════ */
function NumInput({
  label,
  value,
  onChange,
  disabled,
  hint,
}: {
  label: string;
  value: number;
  onChange: (v: number) => void;
  disabled?: boolean;
  hint?: string;
}) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center gap-1 py-2">
      <div className="flex-1">
        <label className="text-sm text-gray-700">{label}</label>
        {hint && <p className="text-xs text-gray-400">{hint}</p>}
      </div>
      <input
        type="number"
        className={`w-full sm:w-48 px-3 py-2 border rounded-lg text-right text-sm ${
          disabled ? 'bg-gray-100 text-gray-500 cursor-not-allowed' : 'border-gray-300 focus:ring-primary-500 focus:border-primary-500'
        }`}
        value={value || 0}
        onChange={(e) => onChange(parseFloat(e.target.value) || 0)}
        disabled={disabled}
      />
    </div>
  );
}

function ResultRow({ label, value, bold, green, red }: {
  label: string; value: number; bold?: boolean; green?: boolean; red?: boolean;
}) {
  const color = green ? 'text-green-700' : red ? 'text-red-600' : 'text-gray-900';
  return (
    <div className={`flex justify-between py-2 px-3 rounded ${bold ? 'bg-primary-50' : 'bg-gray-50'}`}>
      <span className={`text-sm ${bold ? 'font-bold text-primary-900' : 'text-gray-600'}`}>{label}</span>
      <span className={`text-sm font-semibold ${bold ? 'text-primary-700' : color}`}>{fmt(value)}</span>
    </div>
  );
}

/* ════════════════════════════════════════════════════════════════════
   TAB 1 — General Information
   ════════════════════════════════════════════════════════════════════ */
function GeneralInfoTab({
  form,
  updatePartA,
}: {
  form: Itr1FormData;
  updatePartA: (field: string, value: any) => void;
}) {
  const p = form.partA;
  return (
    <div className="card max-w-3xl">
      <h3 className="text-lg font-bold text-gray-900 mb-4">Part A — General Information</h3>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Assessee Name</label>
          <input className="input-field" value={p?.assesseeName || ''} onChange={(e) => updatePartA('assesseeName', e.target.value)} />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">PAN</label>
          <input className="input-field bg-gray-100" value={p?.pan || ''} disabled />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Aadhaar</label>
          <input className="input-field" value={p?.aadhaar || ''} onChange={(e) => updatePartA('aadhaar', e.target.value)} />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Date of Birth</label>
          <input type="date" className="input-field" value={p?.dob || ''} onChange={(e) => updatePartA('dob', e.target.value)} />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
          <input type="email" className="input-field" value={p?.email || ''} onChange={(e) => updatePartA('email', e.target.value)} />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Mobile</label>
          <input className="input-field" value={p?.mobile || ''} onChange={(e) => updatePartA('mobile', e.target.value)} />
        </div>
      </div>

      <div className="mt-6 grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Tax Regime</label>
          <select
            className="input-field"
            value={p?.newTaxRegime ? 'new' : 'old'}
            onChange={(e) => updatePartA('newTaxRegime', e.target.value === 'new')}
          >
            <option value="new">New Regime (u/s 115BAC)</option>
            <option value="old">Old Regime</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Nature of Employment</label>
          <select className="input-field" value={p?.natureOfEmployment || 'others'} onChange={(e) => updatePartA('natureOfEmployment', e.target.value)}>
            <option value="CG">Central Government</option>
            <option value="SG">State Government</option>
            <option value="PSU">PSU</option>
            <option value="pensioner">Pensioner</option>
            <option value="others">Others</option>
            <option value="not_applicable">Not Applicable</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Filing Status</label>
          <select className="input-field" value={p?.filingStatus || 'original'} onChange={(e) => updatePartA('filingStatus', e.target.value)}>
            <option value="original">Original u/s 139(1)</option>
            <option value="revised">Revised u/s 139(5)</option>
            <option value="belated">Belated u/s 139(4)</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Residential Status</label>
          <select className="input-field" value={p?.residentialStatus || 'resident'} onChange={(e) => updatePartA('residentialStatus', e.target.value)}>
            <option value="resident">Resident</option>
            <option value="nri">Non-Resident</option>
          </select>
        </div>
      </div>

      {p?.newTaxRegime && (
        <div className="mt-4 bg-blue-50 border border-blue-200 rounded-lg p-3 text-sm text-blue-800">
          <strong>New Regime (115BAC):</strong> Standard deduction ₹75,000. Only deduction u/s 80CCD(2) is allowed. No HRA exemption or HP interest for self-occupied property.
        </div>
      )}
      {!p?.newTaxRegime && (
        <div className="mt-4 bg-orange-50 border border-orange-200 rounded-lg p-3 text-sm text-orange-800">
          <strong>Old Regime:</strong> Standard deduction ₹50,000. All Chapter VI-A deductions available. HP interest up to ₹2,00,000 for self-occupied.
        </div>
      )}
    </div>
  );
}

/* ════════════════════════════════════════════════════════════════════
   TAB 2 — Salary
   ════════════════════════════════════════════════════════════════════ */
function SalaryTab({ form, updateSalary }: { form: Itr1FormData; updateSalary: (f: string, v: number) => void }) {
  const s = form.scheduleSalary;
  const isNew = form.partA?.newTaxRegime;
  return (
    <div className="card max-w-3xl">
      <h3 className="text-lg font-bold text-gray-900 mb-1">Schedule Salary</h3>
      <p className="text-xs text-gray-500 mb-4">Income chargeable under head "Salaries" [Rules 59-62]</p>

      <div className="space-y-1">
        <NumInput label="(ia) Salary as per sec 17(1)" value={s?.salaryU17_1} onChange={(v) => updateSalary('salaryU17_1', v)} />
        <NumInput label="(ib) Perquisites u/s 17(2)" value={s?.perquisitesU17_2} onChange={(v) => updateSalary('perquisitesU17_2', v)} />
        <NumInput label="(ic) Profits in lieu of salary u/s 17(3)" value={s?.profitsU17_3} onChange={(v) => updateSalary('profitsU17_3', v)} />
        <NumInput label="(id) Income u/s 89A (notified)" value={s?.incomeU89A_notified} onChange={(v) => updateSalary('incomeU89A_notified', v)} />
        <NumInput label="(ie) Income u/s 89A (other)" value={s?.incomeU89A_other} onChange={(v) => updateSalary('incomeU89A_other', v)} />
      </div>

      <div className="border-t border-gray-200 mt-4 pt-3 space-y-1">
        <ResultRow label="(ii) Gross Salary [Rule 59]" value={s?.grossSalary || 0} bold />
      </div>

      <div className="border-t border-gray-200 mt-4 pt-3 space-y-1">
        <NumInput label="(iia) Allowances exempt u/s 10" value={s?.allowancesExemptU10} onChange={(v) => updateSalary('allowancesExemptU10', v)} hint="Cannot exceed Gross Salary [Rule 63]" />
        <NumInput label="(iib) Relief u/s 89A" value={s?.reliefU89A} onChange={(v) => updateSalary('reliefU89A', v)} />
        <ResultRow label="(iii) Net Salary [Rule 60]" value={s?.netSalary || 0} />
      </div>

      <div className="border-t border-gray-200 mt-4 pt-3 space-y-1">
        <h4 className="text-sm font-semibold text-gray-800 mb-2">Deductions u/s 16</h4>
        <NumInput label={`(iva) Standard deduction u/s 16(ia) [max ${isNew ? '₹75,000' : '₹50,000'}]`} value={s?.standardDeduction} onChange={(v) => updateSalary('standardDeduction', v)} hint={isNew ? 'Rule 224 — New regime' : 'Rule 112 — Old regime'} />
        <NumInput label="(ivb) Entertainment allowance u/s 16(ii)" value={s?.entertainmentAllowance} onChange={(v) => updateSalary('entertainmentAllowance', v)} disabled={isNew} hint={isNew ? 'Not allowed in new regime [Rule 164]' : 'Only CG/SG/PSU [Rule 58]'} />
        <NumInput label="(ivc) Professional tax u/s 16(iii)" value={s?.professionalTax} onChange={(v) => updateSalary('professionalTax', v)} disabled={isNew} hint={isNew ? 'Not allowed in new regime [Rule 169]' : ''} />
      </div>

      <div className="border-t border-gray-200 mt-4 pt-3 space-y-1">
        <ResultRow label="(iv) Total deductions u/s 16 [Rule 61]" value={s?.totalDeductionsU16 || 0} />
        <ResultRow label="(v) Income chargeable under Salaries [Rule 62]" value={s?.incomeFromSalary || 0} bold />
      </div>
    </div>
  );
}

/* ════════════════════════════════════════════════════════════════════
   TAB 3 — House Property
   ════════════════════════════════════════════════════════════════════ */
function HousePropertyTab({ form, updateHP }: { form: Itr1FormData; updateHP: (f: string, v: any) => void }) {
  const hp = form.scheduleHP;
  const isNew = form.partA?.newTaxRegime;
  const isSO = hp?.propertyType === 'self_occupied';

  return (
    <div className="card max-w-3xl">
      <h3 className="text-lg font-bold text-gray-900 mb-1">Schedule House Property</h3>
      <p className="text-xs text-gray-500 mb-4">Income from house property [Rules 43-49]</p>

      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-1">Property Type</label>
        <select className="input-field w-full sm:w-64" value={hp?.propertyType || 'self_occupied'} onChange={(e) => updateHP('propertyType', e.target.value)}>
          <option value="self_occupied">Self Occupied</option>
          <option value="let_out">Let Out</option>
          <option value="deemed_let_out">Deemed Let Out</option>
        </select>
      </div>

      {!isSO && (
        <div className="space-y-1">
          <NumInput label="(i) Gross Rent Received" value={hp?.grossRent} onChange={(v) => updateHP('grossRent', v)} />
          <NumInput label="(ii) Municipal Tax Paid" value={hp?.municipalTaxPaid} onChange={(v) => updateHP('municipalTaxPaid', v)} />
          <ResultRow label="(iii) Annual Value [Rule 46]" value={hp?.annualValue || 0} />
          <ResultRow label="(iv) 30% Standard Deduction [Rule 43]" value={hp?.standardDeduction30Pct || 0} />
        </div>
      )}

      <div className="space-y-1 mt-3">
        <NumInput
          label="(v) Interest on borrowed capital u/s 24(b)"
          value={hp?.interestOnLoanU24b}
          onChange={(v) => updateHP('interestOnLoanU24b', v)}
          disabled={isNew && isSO}
          hint={
            isNew && isSO
              ? 'Not allowed for self-occupied in new regime [Rules 163/263]'
              : isSO
              ? 'Max ₹2,00,000 for self-occupied [Rule 48]'
              : ''
          }
        />
      </div>

      {!isSO && (
        <NumInput label="(vi) Arrears/Unrealised Rent Received" value={hp?.arrearsUnrealizedRent} onChange={(v) => updateHP('arrearsUnrealizedRent', v)} />
      )}

      <div className="border-t border-gray-200 mt-4 pt-3">
        <ResultRow label="(vii) Income from House Property [Rule 47]" value={hp?.incomeFromHP || 0} bold />
      </div>
    </div>
  );
}

/* ════════════════════════════════════════════════════════════════════
   TAB 4 — Other Sources
   ════════════════════════════════════════════════════════════════════ */
function OtherSourcesTab({ form, updateOS }: { form: Itr1FormData; updateOS: (f: string, v: number) => void }) {
  const os = form.scheduleOS;
  const isNew = form.partA?.newTaxRegime;
  return (
    <div className="card max-w-3xl">
      <h3 className="text-lg font-bold text-gray-900 mb-1">Schedule Other Sources</h3>
      <p className="text-xs text-gray-500 mb-4">Income from other sources [Rules 50-56]</p>

      <div className="space-y-1">
        <NumInput label="Interest from Savings Account" value={os?.savingsInterest} onChange={(v) => updateOS('savingsInterest', v)} />
        <NumInput label="Interest from Deposits" value={os?.depositInterest} onChange={(v) => updateOS('depositInterest', v)} />
        <NumInput label="Interest from IT Refund" value={os?.incomeTaxRefundInterest} onChange={(v) => updateOS('incomeTaxRefundInterest', v)} />
        <NumInput label="Family Pension" value={os?.familyPension} onChange={(v) => updateOS('familyPension', v)} />
        <NumInput label="Dividend Income" value={os?.dividendIncome} onChange={(v) => updateOS('dividendIncome', v)} />
        <NumInput label="Other Income" value={os?.otherIncome} onChange={(v) => updateOS('otherIncome', v)} />
      </div>

      <div className="border-t border-gray-200 mt-4 pt-3 space-y-1">
        <ResultRow label="(i) Gross Other Sources [Rule 52]" value={os?.grossOtherSources || 0} />
        <ResultRow label={`(ii) Deduction u/s 57(iia) Family Pension [max ${isNew ? '₹25,000' : '₹15,000'}]`} value={os?.deductionU57iia || 0} green />
        <ResultRow label="(iii) Income from Other Sources" value={os?.incomeFromOtherSources || 0} bold />
      </div>

      {(os?.dividendIncome || 0) > 0 && (
        <div className="border-t border-gray-200 mt-4 pt-3">
          <h4 className="text-sm font-semibold text-gray-700 mb-2">Quarterly Dividend Breakup [Rule 146]</h4>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            <div>
              <label className="text-xs text-gray-500">Q1 (Apr-Jun)</label>
              <input type="number" className="input-field text-right text-sm" value={os?.dividendQ1 || 0} onChange={(e) => updateOS('dividendQ1', parseFloat(e.target.value) || 0)} />
            </div>
            <div>
              <label className="text-xs text-gray-500">Q2 (Jul-Sep)</label>
              <input type="number" className="input-field text-right text-sm" value={os?.dividendQ2 || 0} onChange={(e) => updateOS('dividendQ2', parseFloat(e.target.value) || 0)} />
            </div>
            <div>
              <label className="text-xs text-gray-500">Q3 (Oct-Dec)</label>
              <input type="number" className="input-field text-right text-sm" value={os?.dividendQ3 || 0} onChange={(e) => updateOS('dividendQ3', parseFloat(e.target.value) || 0)} />
            </div>
            <div>
              <label className="text-xs text-gray-500">Q4 (Jan-Mar)</label>
              <input type="number" className="input-field text-right text-sm" value={os?.dividendQ4 || 0} onChange={(e) => updateOS('dividendQ4', parseFloat(e.target.value) || 0)} />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

/* ════════════════════════════════════════════════════════════════════
   TAB 5 — Deductions (Chapter VI-A)
   ════════════════════════════════════════════════════════════════════ */
function DeductionsTab({ form, updateDed }: { form: Itr1FormData; updateDed: (f: string, v: number) => void }) {
  const d = form.deductionsVIA;
  const isNew = form.partA?.newTaxRegime;

  return (
    <div className="card max-w-3xl">
      <h3 className="text-lg font-bold text-gray-900 mb-1">Deductions under Chapter VI-A</h3>
      <p className="text-xs text-gray-500 mb-4">[Rules 115-176]</p>

      {isNew && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-sm text-blue-800 mb-4">
          <strong>New Regime:</strong> Only deduction u/s 80CCD(2) (employer contribution to NPS) is allowed. All other deductions are disabled.
        </div>
      )}

      <h4 className="text-sm font-semibold text-gray-800 mb-2">Section 80C / 80CCC / 80CCD</h4>
      <div className="space-y-1">
        <NumInput label="80C (PPF, ELSS, LIC, etc.)" value={d?.section80C} onChange={(v) => updateDed('section80C', v)} disabled={isNew} hint="Max ₹1,50,000 combined" />
        <NumInput label="80CCC (Pension Fund)" value={d?.section80CCC} onChange={(v) => updateDed('section80CCC', v)} disabled={isNew} />
        <NumInput label="80CCD(1) (Employee NPS)" value={d?.section80CCD1} onChange={(v) => updateDed('section80CCD1', v)} disabled={isNew} />
        <ResultRow label="Total 80C+80CCC+80CCD(1) [max ₹1,50,000]" value={d?.total80C_CCC_CCD1 || 0} />
        <NumInput label="80CCD(1B) (Additional NPS)" value={d?.section80CCD1B} onChange={(v) => updateDed('section80CCD1B', v)} disabled={isNew} hint="Max ₹50,000 [Rule 115]" />
        <NumInput label="80CCD(2) (Employer NPS)" value={d?.section80CCD2} onChange={(v) => updateDed('section80CCD2', v)} hint="Allowed in both regimes [Rule 225]" />
      </div>

      <h4 className="text-sm font-semibold text-gray-800 mt-6 mb-2">Health & Disability</h4>
      <div className="space-y-1">
        <NumInput label="80D (Medical Insurance)" value={d?.section80D} onChange={(v) => updateDed('section80D', v)} disabled={isNew} />
        <NumInput label="80DD (Disability of Dependent)" value={d?.section80DD} onChange={(v) => updateDed('section80DD', v)} disabled={isNew} />
        <NumInput label="80DDB (Medical Treatment)" value={d?.section80DDB} onChange={(v) => updateDed('section80DDB', v)} disabled={isNew} />
        <NumInput label="80U (Person with Disability)" value={d?.section80U} onChange={(v) => updateDed('section80U', v)} disabled={isNew} hint="₹75,000 or ₹1,25,000 (severe)" />
      </div>

      <h4 className="text-sm font-semibold text-gray-800 mt-6 mb-2">Education & Housing</h4>
      <div className="space-y-1">
        <NumInput label="80E (Education Loan Interest)" value={d?.section80E} onChange={(v) => updateDed('section80E', v)} disabled={isNew} />
        <NumInput label="80EE (Home Loan Interest)" value={d?.section80EE} onChange={(v) => updateDed('section80EE', v)} disabled={isNew} hint="Max ₹50,000 [Rule 122]" />
        <NumInput label="80EEA (Affordable Housing)" value={d?.section80EEA} onChange={(v) => updateDed('section80EEA', v)} disabled={isNew} hint="Max ₹1,50,000 [Rule 123]" />
        <NumInput label="80EEB (Electric Vehicle Loan)" value={d?.section80EEB} onChange={(v) => updateDed('section80EEB', v)} disabled={isNew} hint="Max ₹1,50,000 [Rule 125]" />
        <NumInput label="80GG (Rent Paid)" value={d?.section80GG} onChange={(v) => updateDed('section80GG', v)} disabled={isNew} hint="Max ₹60,000 [Rule 114]" />
      </div>

      <h4 className="text-sm font-semibold text-gray-800 mt-6 mb-2">Donations & Interest</h4>
      <div className="space-y-1">
        <NumInput label="80G (Donations)" value={d?.section80G} onChange={(v) => updateDed('section80G', v)} disabled={isNew} />
        <NumInput label="80GGA (Scientific Research)" value={d?.section80GGA} onChange={(v) => updateDed('section80GGA', v)} disabled={isNew} />
        <NumInput label="80GGC (Political Party)" value={d?.section80GGC} onChange={(v) => updateDed('section80GGC', v)} disabled={isNew} />
        <NumInput label="80TTA (Savings Interest)" value={d?.section80TTA} onChange={(v) => updateDed('section80TTA', v)} disabled={isNew} hint="Max ₹10,000" />
        <NumInput label="80TTB (Senior Citizen Interest)" value={d?.section80TTB} onChange={(v) => updateDed('section80TTB', v)} disabled={isNew} hint="Max ₹50,000" />
        <NumInput label="80CCH (Agnipath Scheme)" value={d?.section80CCH} onChange={(v) => updateDed('section80CCH', v)} disabled={isNew} />
      </div>

      <div className="border-t border-gray-200 mt-6 pt-3">
        <ResultRow label="Total Deductions under Chapter VI-A" value={d?.totalDeductions || 0} bold />
      </div>
    </div>
  );
}

/* ════════════════════════════════════════════════════════════════════
   TAB 6 — Taxes Paid
   ════════════════════════════════════════════════════════════════════ */
function TaxesPaidTab({ form, updateTax }: { form: Itr1FormData; updateTax: (f: string, v: number) => void }) {
  const tp = form.taxesPaid;
  return (
    <div className="card max-w-3xl">
      <h3 className="text-lg font-bold text-gray-900 mb-1">Schedule Taxes Paid</h3>
      <p className="text-xs text-gray-500 mb-4">Tax Deducted / Collected / Paid [Rules 95-111]</p>

      <div className="space-y-1">
        <NumInput label="TDS on Salary (TDS1)" value={tp?.tdsOnSalary} onChange={(v) => updateTax('tdsOnSalary', v)} hint="Cannot exceed Gross Salary [Rule 193]" />
        <NumInput label="TDS other than Salary (TDS2)" value={tp?.tdsOtherThanSalary} onChange={(v) => updateTax('tdsOtherThanSalary', v)} />
        <NumInput label="TDS u/s 194N (TDS3)" value={tp?.tds3} onChange={(v) => updateTax('tds3', v)} />
        <NumInput label="Tax Collected at Source (TCS)" value={tp?.tcs} onChange={(v) => updateTax('tcs', v)} />
        <NumInput label="Advance Tax" value={tp?.advanceTax} onChange={(v) => updateTax('advanceTax', v)} />
        <NumInput label="Self-Assessment Tax" value={tp?.selfAssessmentTax} onChange={(v) => updateTax('selfAssessmentTax', v)} />
      </div>

      <div className="border-t border-gray-200 mt-4 pt-3">
        <ResultRow label="Total Taxes Paid [Rule 104]" value={tp?.totalTaxesPaid || 0} bold />
      </div>
    </div>
  );
}

/* ════════════════════════════════════════════════════════════════════
   TAB 7 — Tax Computation Result
   ════════════════════════════════════════════════════════════════════ */
function ComputationTab({ form }: { form: Itr1FormData }) {
  const c = form.computation;
  if (!c || (!c.grossTotalIncome && !c.totalTaxableIncome)) {
    return (
      <div className="card max-w-3xl text-center py-12 text-gray-500">
        <svg className="w-12 h-12 mx-auto mb-3 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
        </svg>
        <p className="text-sm">Click <strong>"Compute Tax"</strong> to calculate your tax liability</p>
      </div>
    );
  }

  const isNew = form.partA?.newTaxRegime;

  return (
    <div className="max-w-4xl space-y-6">
      {/* Income Summary */}
      <div className="card">
        <h3 className="text-lg font-bold text-gray-900 mb-4">Part B — Total Income</h3>
        <div className="space-y-2">
          <ResultRow label="Income from Salary" value={form.scheduleSalary?.incomeFromSalary || 0} />
          <ResultRow label="Income from House Property" value={form.scheduleHP?.incomeFromHP || 0} />
          <ResultRow label="Income from Other Sources" value={form.scheduleOS?.incomeFromOtherSources || 0} />
          <ResultRow label="Gross Total Income" value={c.grossTotalIncome} bold />
          <ResultRow label="Deductions under Chapter VI-A" value={c.totalDeductions} green />
          <ResultRow label="Total Taxable Income (rounded)" value={c.totalTaxableIncome} bold />
        </div>
      </div>

      {/* Old vs New Regime Comparison */}
      <div className="card">
        <h3 className="text-lg font-bold text-gray-900 mb-4">Regime Comparison</h3>
        <div className="grid grid-cols-2 gap-4">
          {[c.oldRegime, c.newRegime].map((r) => {
            if (!r) return null;
            const isSelected = r.regime === c.selectedRegime;
            return (
              <div key={r.regime} className={`rounded-lg border-2 p-4 ${isSelected ? 'border-primary-500 bg-primary-50' : 'border-gray-200'}`}>
                <div className="flex items-center justify-between mb-3">
                  <h4 className="font-bold text-gray-900">{r.regime === 'old' ? 'Old Regime' : 'New Regime (115BAC)'}</h4>
                  {isSelected && <span className="text-xs bg-primary-600 text-white px-2 py-0.5 rounded-full">Selected</span>}
                </div>
                <div className="space-y-1 text-sm">
                  <div className="flex justify-between"><span className="text-gray-600">Tax on Income</span><span className="font-semibold">{fmt(r.taxOnIncome)}</span></div>
                  <div className="flex justify-between"><span className="text-gray-600">Rebate u/s 87A</span><span className="font-semibold text-green-600">-{fmt(r.rebateU87A)}</span></div>
                  {r.marginalRelief > 0 && (
                    <div className="flex justify-between"><span className="text-gray-600">Marginal Relief</span><span className="font-semibold text-green-600">-{fmt(r.marginalRelief)}</span></div>
                  )}
                  <div className="flex justify-between"><span className="text-gray-600">Tax after Rebate</span><span className="font-semibold">{fmt(r.taxAfterRebate)}</span></div>
                  <div className="flex justify-between"><span className="text-gray-600">Surcharge</span><span className="font-semibold">{fmt(r.surcharge)}</span></div>
                  <div className="flex justify-between"><span className="text-gray-600">H&E Cess (4%)</span><span className="font-semibold">{fmt(r.cessAt4Pct)}</span></div>
                  <div className="flex justify-between border-t border-gray-200 pt-2 mt-2">
                    <span className="font-bold text-gray-900">Total Tax</span>
                    <span className="font-bold text-primary-700">{fmt(r.totalTax)}</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Final Summary */}
      <div className="card">
        <h3 className="text-lg font-bold text-gray-900 mb-4">Tax Payable / Refund</h3>
        <div className="space-y-2">
          <ResultRow label="Total Tax Liability" value={c.totalTaxLiability} />
          <ResultRow label="Total Taxes Paid" value={c.totalTaxesPaid} green />
          {c.balanceTaxPayable > 0 && (
            <ResultRow label="Balance Tax Payable" value={c.balanceTaxPayable} bold red />
          )}
          {c.refundDue > 0 && (
            <div className="flex justify-between py-3 px-4 rounded-lg bg-green-100">
              <span className="text-sm font-bold text-green-900">Refund Due</span>
              <span className="text-lg font-bold text-green-700">{fmt(c.refundDue)}</span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
