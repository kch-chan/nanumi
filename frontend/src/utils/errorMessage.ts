import axios from 'axios';
import type { ErrorResponse } from '../types/auth';

// axios 에러에서 백엔드가 내려준 message(ErrorResponse)를 추출한다.
export function getErrorMessage(
  error: unknown,
  fallback = '요청 처리 중 오류가 발생했습니다.',
): string {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    return error.response?.data?.message ?? fallback;
  }
  return fallback;
}
