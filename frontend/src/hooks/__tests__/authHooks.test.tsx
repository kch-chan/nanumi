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
