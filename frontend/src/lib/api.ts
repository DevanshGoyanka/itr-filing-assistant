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
    if (error.response?.status === 401 && typeof window !== 'undefined') {
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

export default api;
