import { useMutation } from '@tanstack/react-query';
import type { AxiosError } from 'axios';
import { login } from '../api/auth';
import { useAuthStore } from '../stores/authStore';
import type { ErrorResponse, LoginRequest, LoginResponse } from '../types/auth';

export function useLogin() {
  const setAuth = useAuthStore((state) => state.setAuth);

  return useMutation<LoginResponse, AxiosError<ErrorResponse>, LoginRequest>({
    mutationFn: login,
    onSuccess: (data) => {
      setAuth({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        user: data.user,
      });
    },
  });
}
