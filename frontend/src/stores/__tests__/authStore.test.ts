import { beforeEach, describe, expect, it } from 'vitest';
import { useAuthStore } from '../authStore';
import type { UserResponse } from '../../types/auth';

const user: UserResponse = {
  id: 1,
  nickname: '나눔이',
  aptName: '행복아파트',
  dong: '101',
  ho: '1502',
  role: 'USER',
};

const STORAGE_KEY = 'nanumi-auth';

const storedState = () => {
  const raw = localStorage.getItem(STORAGE_KEY);
  return raw ? JSON.parse(raw).state : null;
};

describe('authStore', () => {
  beforeEach(() => {
    localStorage.clear();
    useAuthStore.getState().clearAuth();
  });

  it('처음에는 로그인되어 있지 않다', () => {
    const state = useAuthStore.getState();

    expect(state.accessToken).toBeNull();
    expect(state.refreshToken).toBeNull();
    expect(state.user).toBeNull();
    expect(state.isLoggedIn).toBe(false);
  });

  it('로그인하면 토큰과 회원 정보를 담는다', () => {
    useAuthStore.getState().setAuth({
      accessToken: 'access',
      refreshToken: 'refresh',
      user,
    });

    const state = useAuthStore.getState();
    expect(state.accessToken).toBe('access');
    expect(state.refreshToken).toBe('refresh');
    expect(state.user?.nickname).toBe('나눔이');
    expect(state.isLoggedIn).toBe(true);
  });

  // 서버가 리프레시 토큰도 새로 주므로(회전) 둘 다 갈아 끼워야 함
  it('토큰만 갈아 끼울 때 회원 정보는 그대로 둔다', () => {
    useAuthStore.getState().setAuth({ accessToken: 'a1', refreshToken: 'r1', user });

    useAuthStore.getState().setTokens({ accessToken: 'a2', refreshToken: 'r2' });

    const state = useAuthStore.getState();
    expect(state.accessToken).toBe('a2');
    expect(state.refreshToken).toBe('r2');
    expect(state.user?.nickname).toBe('나눔이');
    expect(state.isLoggedIn).toBe(true);
  });

  it('로그아웃하면 전부 비운다', () => {
    useAuthStore.getState().setAuth({ accessToken: 'a', refreshToken: 'r', user });

    useAuthStore.getState().clearAuth();

    const state = useAuthStore.getState();
    expect(state.accessToken).toBeNull();
    expect(state.refreshToken).toBeNull();
    expect(state.user).toBeNull();
    expect(state.isLoggedIn).toBe(false);
  });

  // 액세스 토큰은 수명이 15분이라 남겨 봐야 금방 못 쓰게 됨
  // 새로고침 뒤 첫 요청이 401 이면 axiosInstance 가 리프레시 토큰으로 다시 받아 옴
  it('액세스 토큰은 localStorage 에 남기지 않는다', () => {
    useAuthStore.getState().setAuth({ accessToken: 'access', refreshToken: 'refresh', user });

    const stored = storedState();
    expect(stored.refreshToken).toBe('refresh');
    expect(stored.user.nickname).toBe('나눔이');
    expect(stored.isLoggedIn).toBe(true);
    expect(stored.accessToken).toBeUndefined();
  });

  it('로그아웃하면 저장된 값도 비워진다', () => {
    useAuthStore.getState().setAuth({ accessToken: 'a', refreshToken: 'r', user });

    useAuthStore.getState().clearAuth();

    const stored = storedState();
    expect(stored.refreshToken).toBeNull();
    expect(stored.user).toBeNull();
    expect(stored.isLoggedIn).toBe(false);
  });
});
