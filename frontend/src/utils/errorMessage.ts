import axios from 'axios';
import type { ErrorResponse } from '../types/auth';

// axios 에러에서 백엔드가 내려준 message(ErrorResponse)를 꺼냄
export function getErrorMessage(
  error: unknown,
  fallback = '요청 처리 중 오류가 발생했습니다.',
): string {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    return error.response?.data?.message ?? fallback;
  }
  return fallback;
}

// 백엔드 ErrorCode 의 이름을 꺼냄 (예: DUPLICATE_EMAIL, TERMS_NOT_AGREED)
//
// 화면에서 상황에 따라 다르게 보여 줘야 할 때는 message 가 아니라 이걸 봐야 함.
// 문구로 분기하면(message.includes('이미 사용 중')) 문구를 다듬는 순간 조용히 깨짐
export function getErrorCode(error: unknown): string | null {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    return error.response?.data?.code ?? null;
  }
  return null;
}

// HTTP 상태 코드를 꺼냄. 네트워크가 끊겨 응답 자체가 없으면 null 임
// 429(횟수 제한)처럼 상태만으로 판단할 때 씀
export function getErrorStatus(error: unknown): number | null {
  if (axios.isAxiosError(error)) {
    return error.response?.status ?? null;
  }
  return null;
}
