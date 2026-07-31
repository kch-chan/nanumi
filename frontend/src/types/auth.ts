// 백엔드 DTO(com.nanumi.api.dto.*)와 대응되는 요청/응답 타입

export interface SignupRequest {
  email: string;
  password: string;
  nickname: string;
  aptName: string;
  dong?: string;
  ho?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UserResponse {
  id: number;
  nickname: string;
  aptName: string;
  dong: string | null;
  ho: string | null;
  role: string;
}

export interface SignupResponse {
  message: string;
  user: UserResponse;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: UserResponse;
}

export interface ErrorResponse {
  status: number;
  message: string;
}
