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

// 로그아웃임. 바디는 없고 Authorization: Bearer {accessToken} 이 필요함 (interceptor 가 자동으로 넣어 줌)
export async function logout(): Promise<LogoutResponse> {
  const { data } = await axiosInstance.post<LogoutResponse>('/auth/logout');
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
