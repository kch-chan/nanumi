package com.nanumi.api.dto.response;

import com.nanumi.api.exception.ErrorCode;

public record ErrorResponse(int status, String message) {

  public static ErrorResponse of(ErrorCode errorCode) {
    return new ErrorResponse(errorCode.getStatus().value(), errorCode.getMessage());
  }

  public static ErrorResponse of(int status, String message) {
    return new ErrorResponse(status, message);
  }
}
