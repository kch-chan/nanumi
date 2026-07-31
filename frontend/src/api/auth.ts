import axiosInstance from './axiosInstance';
import type {
  LoginRequest,
  LoginResponse,
  SignupRequest,
  SignupResponse,
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
