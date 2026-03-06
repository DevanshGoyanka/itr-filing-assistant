import { create } from 'zustand';
import { clientApi, ClientResponse } from '@/lib/api';

interface User {
  email: string;
  token: string;
}

interface DashboardState {
  user: User | null;
  clients: ClientResponse[];
  selectedYear: string;
  loading: boolean;
  setUser: (user: User | null) => void;
  fetchClients: () => Promise<void>;
  setSelectedYear: (year: string) => void;
  setLoading: (loading: boolean) => void;
  logout: () => void;
}

export const useDashboardStore = create<DashboardState>((set) => ({
  user: typeof window !== 'undefined'
    ? (() => {
        const token = localStorage.getItem('token');
        const email = localStorage.getItem('email');
        return token && email ? { token, email } : null;
      })()
    : null,
  clients: [],
  selectedYear: 'AY2025-26',
  loading: false,

  setUser: (user) => {
    if (user) {
      localStorage.setItem('token', user.token);
      localStorage.setItem('email', user.email);
    } else {
      localStorage.removeItem('token');
      localStorage.removeItem('email');
    }
    set({ user });
  },

  fetchClients: async () => {
    set({ loading: true });
    try {
      const response = await clientApi.getAll();
      set({ clients: response.data, loading: false });
    } catch {
      set({ loading: false });
    }
  },

  setSelectedYear: (year) => set({ selectedYear: year }),

  setLoading: (loading) => set({ loading }),

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('email');
    set({ user: null, clients: [] });
  },
}));
