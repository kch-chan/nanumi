import { create } from 'zustand';
import type { UserResponse } from '../types/auth';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserResponse | null;
  isLoggedIn: boolean;
  setAuth: (payload: {
    accessToken: string;
    refreshToken: string;
    user: UserResponse;
  }) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: null,
  user: null,
  isLoggedIn: false,
  setAuth: ({ accessToken, refreshToken, user }) =>
    set({ accessToken, refreshToken, user, isLoggedIn: true }),
  clearAuth: () =>
    set({
      accessToken: null,
      refreshToken: null,
      user: null,
      isLoggedIn: false,
    }),
}));
