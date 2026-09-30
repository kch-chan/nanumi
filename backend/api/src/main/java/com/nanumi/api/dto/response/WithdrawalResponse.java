package com.nanumi.api.dto.response;

import java.time.LocalDateTime;

public record WithdrawalResponse(String message, LocalDateTime withdrawnAt) {
  public static WithdrawalResponse of(LocalDateTime withdrawnAt) {
    return new WithdrawalResponse("회원탈퇴가 완료되었습니다.", withdrawnAt);
  }
}
