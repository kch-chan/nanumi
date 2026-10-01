import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  AUTH_STORAGE_KEY,
  readStoredRefreshToken,
  useAuthStore,
} from '../authStore';
import type { UserResponse } from '../../types/auth';

const user: UserResponse = {
  id: 1,
  nickname: '나눔이',
  aptName: '행복아파트',
  dong: '101',
  ho: '1502',
  role: 'USER',
};

// 키를 적어 두지 않고 저장소에서 가져옴
// 적어 두면 authStore 쪽에서 키를 바꿀 때 테스트가 조용히 엉뚱한 키를 보게 됨
const storedState = () => {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
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

// 토큰 재발급 직전에 부르는 함수임. 여기가 틀리면 서버가 재사용 공격으로 보고
// 그 계정의 모든 기기를 로그아웃시킴. 그래서 따로 묶어서 확인함
describe('readStoredRefreshToken', () => {
  beforeEach(() => {
    localStorage.clear();
    useAuthStore.getState().clearAuth();
    vi.restoreAllMocks();
  });

  it('저장된 값이 없으면 null 을 준다', () => {
    localStorage.clear();

    expect(readStoredRefreshToken()).toBeNull();
  });

  it('저장소의 리프레시 토큰을 읽는다', () => {
    useAuthStore.getState().setAuth({ accessToken: 'a', refreshToken: 'r1', user });

    expect(readStoredRefreshToken()).toBe('r1');
  });

  // 다른 탭이 회전시킨 결과는 저장소에만 반영됨. 탭마다 zustand 상태가 따로이기 때문임
  // 메모리를 보면 옛 값을 쓰게 되어 서버가 "없는 토큰" 으로 판단함
  it('메모리가 아니라 저장소의 값을 본다', () => {
    useAuthStore
      .getState()
      .setAuth({ accessToken: 'a', refreshToken: '옛-토큰', user });
    localStorage.setItem(
      AUTH_STORAGE_KEY,
      JSON.stringify({ state: { refreshToken: '새-토큰' }, version: 0 }),
    );

    expect(useAuthStore.getState().refreshToken).toBe('옛-토큰');
    expect(readStoredRefreshToken()).toBe('새-토큰');
  });

  it('JSON 이 깨져 있어도 예외를 던지지 않는다', () => {
    localStorage.setItem(AUTH_STORAGE_KEY, '{깨진 JSON');

    expect(readStoredRefreshToken()).toBeNull();
  });

  it('state 가 없는 구조여도 null 을 준다', () => {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({ version: 0 }));

    expect(readStoredRefreshToken()).toBeNull();
  });

  // 시크릿 모드나 저장소 차단 환경에서는 읽기 자체가 예외를 던짐
  // try/catch 가 없으면 여기서 앱이 죽음
  it('저장소 접근이 막혀 있어도 null 을 준다', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('접근 거부', 'SecurityError');
    });

    expect(readStoredRefreshToken()).toBeNull();
  });
});
