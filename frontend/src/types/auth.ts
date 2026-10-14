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

// 실패 응답임. 서버가 내려주는 칸은 이 둘뿐임
// (backend ErrorResponse.java 는 record (int status, String message) 이고,
//  SecurityConfig.writeError 가 필터 단계에서 쓰는 JSON 도 같은 두 칸임)
//
// 전에는 여기에 code / path / timestamp 가 선언돼 있었고 "화면에서 분기할 때는 code 를 봐야 함"
// 이라고 적혀 있었는데, 서버가 보내지 않는 칸이라 읽으면 언제나 undefined 였음.
// 상황별로 다르게 보여 줘야 하면 status(getErrorStatus)로 분기하거나,
// 서버 ErrorResponse 에 code 를 먼저 추가해야 함
// (docs/api-spec.md 의 "오류 응답" 과 ResponseDtoTest 도 두 칸으로 못 박아 두었으므로 함께 고쳐야 함)
export interface ErrorResponse {
  status: number;
  message: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

// 토큰 재발급 응답임. 리프레시 토큰도 매번 새로 나옴(회전)
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}
