import { useMutation } from '@tanstack/react-query';
import type { AxiosError } from 'axios';
import { logout } from '../api/auth';
import { useAuthStore } from '../stores/authStore';
import type { ErrorResponse, LogoutResponse } from '../types/auth';

export function useLogout() {
  const clearAuth = useAuthStore((state) => state.clearAuth);

  return useMutation<LogoutResponse, AxiosError<ErrorResponse>, void>({
    mutationFn: logout,
    onSuccess: () => {
      clearAuth();
    },
  });
}
