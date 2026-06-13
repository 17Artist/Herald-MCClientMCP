import { create } from 'zustand';
import { api, type StatusResponse } from './api';

interface AppStore {
  status: StatusResponse | null;
  loading: boolean;
  error: string | null;
  pollTimer: ReturnType<typeof setInterval> | null;

  fetchStatus: () => Promise<void>;
  startPolling: () => void;
  stopPolling: () => void;
}

export const useStore = create<AppStore>((set, get) => ({
  status: null,
  loading: false,
  error: null,
  pollTimer: null,

  fetchStatus: async () => {
    try {
      const status = await api.getStatus();
      set({ status, error: null });
    } catch (e) {
      set({ error: (e as Error).message });
    }
  },

  startPolling: () => {
    const { fetchStatus } = get();
    fetchStatus();
    const timer = setInterval(fetchStatus, 3000);
    set({ pollTimer: timer });
  },

  stopPolling: () => {
    const { pollTimer } = get();
    if (pollTimer) clearInterval(pollTimer);
    set({ pollTimer: null });
  },
}));
