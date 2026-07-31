import { useMutation } from '@tanstack/react-query';
import type { AxiosError } from 'axios';
import { signup } from '../api/auth';
import type {
  ErrorResponse,
  SignupRequest,
  SignupResponse,
} from '../types/auth';

export function useSignup() {
  return useMutation<SignupResponse, AxiosError<ErrorResponse>, SignupRequest>({
    mutationFn: signup,
  });
}
