import { useMutation } from '@tanstack/react-query';
import type { AxiosError } from 'axios';
import { logout } from '../api/auth';
import { useAuthStore } from '../stores/authStore';
import type { ErrorResponse, LogoutResponse } from '../types/auth';

export function useLogout() {
  const clearAuth = useAuthStore((state) => state.clearAuth);

  return useMutation<LogoutResponse, AxiosError<ErrorResponse>, void>({
    // 지금 기기만 로그아웃하도록 리프레시 토큰을 같이 보냄.
    // 안 보내면 서버가 이 계정의 모든 기기를 로그아웃함
    mutationFn: () => logout(useAuthStore.getState().refreshToken),
    // onSuccess 가 아니라 onSettled 임
    // 토큰이 이미 만료된 상태에서 로그아웃을 누르면 서버는 401 을 주는데,
    // 그때 로컬 상태를 안 지우면 헤더는 계속 로그인으로 보이고 다시는 로그아웃할 수 없게 됨
    onSettled: () => {
      clearAuth();
    },
  });
}
