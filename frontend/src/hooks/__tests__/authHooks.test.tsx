import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import * as authApi from '../../api/auth';
import { useLogin } from '../useLogin';
import { useLogout } from '../useLogout';
import { useSignup } from '../useSignup';
import { useWithdrawal } from '../useWithdrawal';
import { useAuthStore } from '../../stores/authStore';
import type { UserResponse } from '../../types/auth';

const user: UserResponse = {
  id: 1,
  nickname: '나눔이',
  aptName: '행복아파트',
  dong: '101',
  ho: '1502',
  role: 'USER',
};

// constants/terms.ts 의 effectiveDate 가 그대로 올라감
const TERMS_VERSION = '시행일자 2026년 8월';

// 테스트에서는 실패해도 다시 시도하지 않게 함. 안 그러면 오류 테스트가 느려짐
function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({
    defaultOptions: { mutations: { retry: false }, queries: { retry: false } },
  });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe('인증 훅', () => {
  beforeEach(() => {
    localStorage.clear();
    useAuthStore.getState().clearAuth();
    vi.restoreAllMocks();
  });

  describe('useLogin', () => {
    it('성공하면 토큰과 회원 정보를 저장한다', async () => {
      vi.spyOn(authApi, 'login').mockResolvedValue({
        accessToken: 'access',
        refreshToken: 'refresh',
        user,
      });

      const { result } = renderHook(() => useLogin(), { wrapper });
      result.current.mutate({ email: 'nanumi@example.com', password: 'Ab3!efgh' });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(useAuthStore.getState().isLoggedIn).toBe(true);
      expect(useAuthStore.getState().accessToken).toBe('access');
      expect(useAuthStore.getState().user?.nickname).toBe('나눔이');
    });

    it('실패하면 로그인 상태로 만들지 않는다', async () => {
      vi.spyOn(authApi, 'login').mockRejectedValue(new Error('401'));

      const { result } = renderHook(() => useLogin(), { wrapper });
      result.current.mutate({ email: 'nanumi@example.com', password: 'wrong' });

      await waitFor(() => expect(result.current.isError).toBe(true));
      expect(useAuthStore.getState().isLoggedIn).toBe(false);
    });
  });

  describe('useLogout', () => {
    it('성공하면 로그인 상태를 비운다', async () => {
      useAuthStore.getState().setAuth({ accessToken: 'a', refreshToken: 'r', user });
      vi.spyOn(authApi, 'logout').mockResolvedValue({ message: '로그아웃되었습니다.' });

      const { result } = renderHook(() => useLogout(), { wrapper });
      result.current.mutate();

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(useAuthStore.getState().isLoggedIn).toBe(false);
    });

    // 리프레시 토큰을 같이 보내야 "이 기기만" 로그아웃됨
    // 안 보내면 서버가 그 계정의 모든 기기를 로그아웃시킴(다른 기기 사용자가 갑자기 튕김)
    it('지금 기기만 로그아웃하도록 리프레시 토큰을 보낸다', async () => {
      useAuthStore.getState().setAuth({ accessToken: 'a', refreshToken: 'r1', user });
      const logoutSpy = vi
        .spyOn(authApi, 'logout')
        .mockResolvedValue({ message: '로그아웃되었습니다.' });

      const { result } = renderHook(() => useLogout(), { wrapper });
      result.current.mutate();

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(logoutSpy).toHaveBeenCalledWith('r1');
    });

    // mutationFn 이 getState() 로 읽는 이유임
    // 선택자로 받아 클로저에 담아 두면 재발급으로 토큰이 바뀐 뒤에도 옛 값을 보냄
    it('훅을 만든 뒤 토큰이 바뀌어도 최신 토큰을 보낸다', async () => {
      useAuthStore.getState().setAuth({ accessToken: 'a', refreshToken: 'r1', user });
      const logoutSpy = vi
        .spyOn(authApi, 'logout')
        .mockResolvedValue({ message: '로그아웃되었습니다.' });

      const { result } = renderHook(() => useLogout(), { wrapper });

      // 훅을 만든 뒤에 재발급이 일어난 상황
      useAuthStore.getState().setTokens({ accessToken: 'a2', refreshToken: 'r2' });

      result.current.mutate();

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(logoutSpy).toHaveBeenCalledWith('r2');
    });

    // 토큰이 이미 만료된 상태에서 로그아웃을 누르면 서버는 401 을 줌
    // 그때 로컬 상태를 안 지우면 헤더는 계속 로그인으로 보이고 다시는 로그아웃할 수 없게 됨
    it('서버가 실패해도 로그인 상태를 비운다', async () => {
      useAuthStore.getState().setAuth({ accessToken: 'a', refreshToken: 'r', user });
      vi.spyOn(authApi, 'logout').mockRejectedValue(new Error('401'));

      const { result } = renderHook(() => useLogout(), { wrapper });
      result.current.mutate();

      await waitFor(() => expect(result.current.isError).toBe(true));
      expect(useAuthStore.getState().isLoggedIn).toBe(false);
    });
  });

  describe('useSignup', () => {
    // 가입만 하고 로그인은 따로 함. 여기서 토큰을 담으면 안 됨
    it('성공해도 로그인 상태로 만들지 않는다', async () => {
      vi.spyOn(authApi, 'signup').mockResolvedValue({ message: '가입되었습니다.', user });

      const { result } = renderHook(() => useSignup(), { wrapper });
      result.current.mutate({
        email: 'nanumi@example.com',
        password: 'Ab3!efgh',
        nickname: '나눔이',
        aptName: '행복아파트',
        dong: '101',
        ho: '1502',
        agreements: [
          { key: 'service', version: TERMS_VERSION, agreed: true },
          { key: 'privacy', version: TERMS_VERSION, agreed: true },
          { key: 'marketing', version: TERMS_VERSION, agreed: false },
        ],
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(useAuthStore.getState().isLoggedIn).toBe(false);
    });
  });

  describe('useWithdrawal', () => {
    it('성공하면 로그인 상태를 비운다', async () => {
      useAuthStore.getState().setAuth({ accessToken: 'a', refreshToken: 'r', user });
      vi.spyOn(authApi, 'withdraw').mockResolvedValue({
        message: '탈퇴되었습니다.',
        withdrawnAt: '2026-01-01T00:00:00',
      });

      const { result } = renderHook(() => useWithdrawal(), { wrapper });
      result.current.mutate({ password: 'Ab3!efgh', reason: '이사 갑니다' });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(useAuthStore.getState().isLoggedIn).toBe(false);
    });

    // 비밀번호가 틀려서 탈퇴가 안 됐는데 로그아웃까지 되면 회원이 당황함
    it('실패하면 로그인 상태를 그대로 둔다', async () => {
      useAuthStore.getState().setAuth({ accessToken: 'a', refreshToken: 'r', user });
      vi.spyOn(authApi, 'withdraw').mockRejectedValue(new Error('401'));

      const { result } = renderHook(() => useWithdrawal(), { wrapper });
      result.current.mutate({ password: 'wrong' });

      await waitFor(() => expect(result.current.isError).toBe(true));
      expect(useAuthStore.getState().isLoggedIn).toBe(true);
    });
  });
});
