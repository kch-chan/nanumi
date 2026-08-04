import { useMutation } from '@tanstack/react-query';
import type { AxiosError } from 'axios';
import { withdraw } from '../api/auth';
import { useAuthStore } from '../stores/authStore';
import type {
  ErrorResponse,
  WithdrawalRequest,
  WithdrawalResponse,
} from '../types/auth';

export function useWithdrawal() {
  const clearAuth = useAuthStore((state) => state.clearAuth);

  return useMutation<
    WithdrawalResponse,
    AxiosError<ErrorResponse>,
    WithdrawalRequest
  >({
    mutationFn: withdraw,
    onSuccess: () => {
      clearAuth();
    },
  });
}
