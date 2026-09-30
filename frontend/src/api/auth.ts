import axiosInstance from './axiosInstance';
import type {
  LoginRequest,
  LoginResponse,
  LogoutResponse,
  SignupRequest,
  SignupResponse,
  WithdrawalRequest,
  WithdrawalResponse,
} from '../types/auth';

export async function signup(payload: SignupRequest): Promise<SignupResponse> {
  const { data } = await axiosInstance.post<SignupResponse>(
    '/auth/signup',
    payload,
  );
  return data;
}

export async function login(payload: LoginRequest): Promise<LoginResponse> {
  const { data } = await axiosInstance.post<LoginResponse>(
    '/auth/login',
    payload,
  );
  return data;
}

// 로그아웃임. Authorization: Bearer {accessToken} 이 필요함 (interceptor 가 자동으로 넣어 줌)
//
// 리프레시 토큰을 같이 보내면 지금 쓰는 기기만 로그아웃함.
// 안 보내면 서버가 이 계정의 모든 기기를 로그아웃함
export async function logout(refreshToken?: string | null): Promise<LogoutResponse> {
  const { data } = await axiosInstance.post<LogoutResponse>('/auth/logout', {
    refreshToken: refreshToken ?? undefined,
  });
  return data;
}

// 회원탈퇴(Soft Delete)임. 비밀번호를 다시 확인하고 Authorization 헤더도 필요함
export async function withdraw(
  payload: WithdrawalRequest,
): Promise<WithdrawalResponse> {
  const { data } = await axiosInstance.post<WithdrawalResponse>(
    '/auth/withdrawal',
    payload,
  );
  return data;
}
