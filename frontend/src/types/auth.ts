// 약관 동의 한 건임. 서버가 이걸 terms_agreements 에 한 줄로 남김
//
// agreed 가 false 인 것도 보내야 함. 선택 약관을 "물어봤고 거부했다" 는 사실 자체가
// 마케팅 발송 여부를 증명하는 기록이 됨
export interface TermsAgreementRequest {
  key: string;
  // 동의한 약관의 판. constants/terms.ts 의 effectiveDate 를 그대로 보냄
  version: string;
  agreed: boolean;
}

export interface SignupRequest {
  email: string;
  password: string;
  nickname: string;
  aptName: string;
  dong?: string;
  ho?: string;
  // 서버가 필수 약관 동의를 다시 확인함. 비어 있으면 400 이 남
  agreements: TermsAgreementRequest[];
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface WithdrawalRequest {
  password: string;
  reason?: string;
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

export interface LogoutResponse {
  message: string;
}

export interface WithdrawalResponse {
  message: string;
  withdrawnAt: string;
}

export interface ErrorResponse {
  status: number;
  // 백엔드 ErrorCode 의 이름임(예: DUPLICATE_EMAIL, TERMS_NOT_AGREED)
  // 문구는 바뀔 수 있지만 코드는 그대로이므로, 화면에서 분기할 때는 message 대신 이걸 봐야 함
  code: string;
  message: string;
  path?: string;
  timestamp?: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

// 토큰 재발급 응답임. 리프레시 토큰도 매번 새로 나옴(회전)
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}
