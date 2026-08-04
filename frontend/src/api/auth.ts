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

// 로그아웃: 바디 없음. Authorization: Bearer {accessToken} 필요 (interceptor 자동 주입)
export async function logout(): Promise<LogoutResponse> {
  const { data } = await axiosInstance.post<LogoutResponse>('/auth/logout');
  return data;
}

// 회원탈퇴(Soft Delete): 비밀번호 재확인 필요. Authorization 헤더 필요
export async function withdraw(
  payload: WithdrawalRequest,
): Promise<WithdrawalResponse> {
  const { data } = await axiosInstance.post<WithdrawalResponse>(
    '/auth/withdrawal',
    payload,
  );
  return data;
}
