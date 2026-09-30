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

export const AUTH_STORAGE_KEY = 'nanumi-auth';

// 새로고침해도 로그인이 풀리지 않도록 리프레시 토큰과 회원 정보만 localStorage 에 남김
//
// 액세스 토큰은 남기지 않음. 수명이 15분이라 남겨 봐야 금방 못 쓰게 되고,
// 새로고침 뒤 첫 요청이 401 이 나면 axiosInstance 가 리프레시 토큰으로 알아서 다시 받아 옴
//
// 리프레시 토큰을 localStorage 에 두는 건 XSS 가 나면 그대로 털린다는 뜻이라 안전한 선택은 아님
// 제대로 하려면 httpOnly 쿠키로 옮기고 CSRF 방어를 얹어야 하는데 서버까지 같이 고쳐야 함
//
// 다만 지금 구조에서는 쿠키로 옮겨도 동작하지 않음. 프런트(vercel.app)와 백엔드(onrender.com)가
// 등록 도메인부터 다르기 때문에 그 쿠키는 서드파티 쿠키가 되고, 사파리는 이미 막고 있고 크롬도 단계적으로 막는 중임.
// 쿠키로 가려면 두 쪽을 같은 도메인 아래로(예: nanumi.com / api.nanumi.com) 먼저 옮겨야 함
//
// 그래서 지금은 "훔칠 스크립트가 아예 못 돌게" 하는 쪽으로 막아 둠(frontend/vercel.json 의 CSP).
// 도메인을 붙이고 나면 쿠키 전환을 다시 다룰 것
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
      name: AUTH_STORAGE_KEY,
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        refreshToken: state.refreshToken,
        user: state.user,
        isLoggedIn: state.isLoggedIn,
      }),
    },
  ),
);

// 저장소에 실제로 들어 있는 리프레시 토큰을 읽음
//
// 왜 메모리(getState) 가 아니라 저장소인가: 탭마다 zustand 상태가 따로 있어서, 다른 탭이
// 재발급으로 토큰을 갈아 끼워도 이 탭의 메모리는 옛 값을 들고 있음.
// 그 옛 값으로 재발급을 부르면 서버가 "없는 토큰" 으로 보고 재사용 공격으로 판단해
// 그 계정의 모든 기기를 로그아웃시킴. 그래서 부르기 직전에 저장소를 다시 봄
export function readStoredRefreshToken(): string | null {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY);
    if (!raw) {
      return null;
    }
    // zustand persist 가 { state, version } 모양으로 감싸서 넣음
    const parsed = JSON.parse(raw) as { state?: { refreshToken?: string | null } };
    return parsed.state?.refreshToken ?? null;
  } catch {
    // 시크릿 모드나 저장소 차단 환경에서는 읽기 자체가 예외를 던짐
    return null;
  }
}
