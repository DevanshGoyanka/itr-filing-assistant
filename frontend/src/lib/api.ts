import axios from 'axios';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add auth token to requests
api.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

// Handle 401 responses
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if ((error.response?.status === 401 || error.response?.status === 403) && typeof window !== 'undefined') {
      localStorage.removeItem('token');
      localStorage.removeItem('email');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export interface AuthRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  expiresIn: number;
}

export interface ClientRequest {
  pan: string;
  name: string;
  email?: string;
  mobile?: string;
  aadhaar?: string;
  dob?: string;
}

export interface ClientResponse {
  id: number;
  pan: string;
  name: string;
  email?: string;
  mobile?: string;
  aadhaar?: string;
  dob?: string;
  years: { year: string; status: string | null }[];
}

export interface PrefillResponse {
  clientId: number;
  clientName: string;
  pan: string;
  assessmentYear: string;
  status: string;
  incomeSummary: {
    assesseeName: string;
    panNumber: string;
    dob: string;
    grossSalary: number;
    section80C: number;
    section80D: number;
    totalIncome: number;
    otherSources: number;
  };
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

// ═══════════════════════════════════════════════════════════════
// ITR-1 Form Data Types (mirrors backend Itr1FormData DTO)
// ═══════════════════════════════════════════════════════════════
export interface Itr1FormData {
  partA: PartA_GeneralInfo;
  scheduleSalary: ScheduleSalary;
  scheduleHP: ScheduleHouseProperty;
  scheduleOS: ScheduleOtherSources;
  scheduleExempt: ScheduleExemptIncome;
  deductionsVIA: DeductionsVIA;
  computation: TaxComputation;
  taxesPaid: ScheduleTaxesPaid;
  validationErrors: string[];
  validationWarnings: string[];
}

export interface PartA_GeneralInfo {
  assesseeName: string;
  pan: string;
  aadhaar: string;
  dob: string;
  email: string;
  mobile: string;
  address: string;
  assessmentYear: string;
  filingStatus: string;
  residentialStatus: string;
  newTaxRegime: boolean;
  natureOfEmployment: string;
  filingSection: string;
}

export interface ScheduleSalary {
  salaryU17_1: number;
  perquisitesU17_2: number;
  profitsU17_3: number;
  incomeU89A_notified: number;
  incomeU89A_other: number;
  grossSalary: number;
  allowancesExemptU10: number;
  reliefU89A: number;
  netSalary: number;
  standardDeduction: number;
  entertainmentAllowance: number;
  professionalTax: number;
  totalDeductionsU16: number;
  incomeFromSalary: number;
  exemptAllowances: ExemptAllowance[];
}

export interface ExemptAllowance {
  section: string;
  description: string;
  amount: number;
}

export interface ScheduleHouseProperty {
  propertyType: string;
  grossRent: number;
  municipalTaxPaid: number;
  annualValue: number;
  standardDeduction30Pct: number;
  interestOnLoanU24b: number;
  arrearsUnrealizedRent: number;
  incomeFromHP: number;
}

export interface ScheduleOtherSources {
  savingsInterest: number;
  depositInterest: number;
  incomeTaxRefundInterest: number;
  familyPension: number;
  dividendIncome: number;
  otherIncome: number;
  incomeU89A: number;
  grossOtherSources: number;
  deductionU57iia: number;
  incomeFromOtherSources: number;
  dividendQ1: number;
  dividendQ2: number;
  dividendQ3: number;
  dividendQ4: number;
}

export interface ScheduleExemptIncome {
  agricultureIncome: number;
  exemptInterestIncome: number;
  ltcgU112A_exempt: number;
  ltcgU112A_cost: number;
  ltcgU112A_sale: number;
  otherExemptIncome: number;
  totalExemptIncome: number;
}

export interface DeductionsVIA {
  section80C: number;
  section80CCC: number;
  section80CCD1: number;
  total80C_CCC_CCD1: number;
  section80CCD1B: number;
  section80CCD2: number;
  section80D: number;
  section80DD: number;
  section80DDB: number;
  section80E: number;
  section80EE: number;
  section80EEA: number;
  section80EEB: number;
  section80G: number;
  section80GG: number;
  section80GGA: number;
  section80GGC: number;
  section80TTA: number;
  section80TTB: number;
  section80U: number;
  section80CCH: number;
  totalDeductions: number;
}

export interface TaxComputation {
  grossTotalIncome: number;
  totalDeductions: number;
  totalTaxableIncome: number;
  oldRegime: RegimeTaxBreakdown;
  newRegime: RegimeTaxBreakdown;
  selectedRegime: string;
  taxOnIncome: number;
  rebateU87A: number;
  taxAfterRebate: number;
  surcharge: number;
  cessAt4Pct: number;
  totalTaxLiability: number;
  reliefU89: number;
  totalTaxAfterRelief: number;
  totalTaxesPaid: number;
  balanceTaxPayable: number;
  refundDue: number;
  interestPayable: number;
  totalTaxAndInterest: number;
}

export interface RegimeTaxBreakdown {
  regime: string;
  taxableIncome: number;
  taxOnIncome: number;
  rebateU87A: number;
  taxAfterRebate: number;
  surcharge: number;
  cessAt4Pct: number;
  totalTax: number;
  marginalRelief: number;
}

export interface ScheduleTaxesPaid {
  tdsOnSalary: number;
  tdsOtherThanSalary: number;
  tds3: number;
  tcs: number;
  advanceTax: number;
  selfAssessmentTax: number;
  totalTaxesPaid: number;
}

// Auth APIs
export const authApi = {
  register: (data: AuthRequest) => api.post<AuthResponse>('/auth/register', data),
  login: (data: AuthRequest) => api.post<AuthResponse>('/auth/login', data),
};

// Client APIs
export const clientApi = {
  getAll: () => api.get<ClientResponse[]>('/clients'),
  getById: (id: number) => api.get<ClientResponse>(`/clients/${id}`),
  create: (data: ClientRequest) => api.post<ClientResponse>('/clients', data),
  update: (id: number, data: ClientRequest) => api.put<ClientResponse>(`/clients/${id}`, data),
  getYears: (id: number) => api.get<{ year: string; status: string | null }[]>(`/clients/${id}/years`),
};

// Prefill APIs
export const prefillApi = {
  uploadFile: (clientId: number, year: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post<PrefillResponse>(`/clients/${clientId}/prefill/${year}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  uploadJson: (clientId: number, year: string, json: string) =>
    api.post<PrefillResponse>(`/clients/${clientId}/prefill/${year}`, json, {
      headers: { 'Content-Type': 'application/json' },
    }),
  getData: (clientId: number, year: string) =>
    api.get<PrefillResponse>(`/clients/${clientId}/prefill/${year}`),
};

// ITR-1 Form APIs
export const itrApi = {
  getFormData: (clientId: number, year: string) =>
    api.get<Itr1FormData>(`/clients/${clientId}/itr/${year}`),
  saveFormData: (clientId: number, year: string, data: Itr1FormData) =>
    api.put<Itr1FormData>(`/clients/${clientId}/itr/${year}`, data),
  computeForm: (clientId: number, year: string, data: Itr1FormData) =>
    api.post<Itr1FormData>(`/clients/${clientId}/itr/${year}/compute`, data),
  downloadJson: (clientId: number, year: string) =>
    api.get(`/clients/${clientId}/itr/${year}/download`),
  downloadExcel: (clientId: number, year: string) =>
    api.get(`/clients/${clientId}/itr/${year}/download-excel`, { responseType: 'blob' }),
  downloadPdf: (clientId: number, year: string) =>
    api.get(`/clients/${clientId}/itr/${year}/download-pdf`, { responseType: 'blob' }),
};

export default api;
