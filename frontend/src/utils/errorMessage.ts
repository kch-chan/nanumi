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

// getErrorCode 는 지웠음
//
// 백엔드 ErrorResponse 는 (status, message) 두 칸뿐이라 code 를 보내지 않음.
// 그래서 error.response.data.code 는 언제나 undefined 였고, 이 함수는 항상 null 을 돌려줬음.
// "문구 대신 code 로 분기하라" 는 주석까지 달려 있어서 그대로 믿고 쓰면 조용히 깨지는 자리였음
//
// 상황별 분기가 필요하면 아래 getErrorStatus 로 상태 코드를 보거나,
// 서버 ErrorResponse 에 code 를 먼저 추가해야 함(docs/api-spec.md 와 ResponseDtoTest 도 함께 고칠 것)

// HTTP 상태 코드를 꺼냄. 네트워크가 끊겨 응답 자체가 없으면 null 임
// 429(횟수 제한)처럼 상태만으로 판단할 때 씀
export function getErrorStatus(error: unknown): number | null {
  if (axios.isAxiosError(error)) {
    return error.response?.status ?? null;
  }
  return null;
}
