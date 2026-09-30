import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
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
  setTokens: (payload: { accessToken: string; refreshToken: string }) => void;
  clearAuth: () => void;
}

// 새로고침해도 로그인이 풀리지 않도록 리프레시 토큰과 회원 정보만 localStorage 에 남김
//
// 액세스 토큰은 남기지 않음. 수명이 15분이라 남겨 봐야 금방 못 쓰게 되고,
// 새로고침 뒤 첫 요청이 401 이 나면 axiosInstance 가 리프레시 토큰으로 알아서 다시 받아 옴
//
// 리프레시 토큰을 localStorage 에 두는 건 XSS 가 나면 그대로 털린다는 뜻이라 안전한 선택은 아님
// 제대로 하려면 httpOnly 쿠키로 옮기고 CSRF 방어를 얹어야 하는데 서버까지 같이 고쳐야 함
// 지금은 "새로고침하면 로그아웃되는" 문제를 먼저 막아 두고, 쿠키 전환은 따로 다루기로 함
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isLoggedIn: false,
      setAuth: ({ accessToken, refreshToken, user }) =>
        set({ accessToken, refreshToken, user, isLoggedIn: true }),
      setTokens: ({ accessToken, refreshToken }) =>
        set({ accessToken, refreshToken }),
      clearAuth: () =>
        set({
          accessToken: null,
          refreshToken: null,
          user: null,
          isLoggedIn: false,
        }),
    }),
    {
      name: 'nanumi-auth',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        refreshToken: state.refreshToken,
        user: state.user,
        isLoggedIn: state.isLoggedIn,
      }),
    },
  ),
);
