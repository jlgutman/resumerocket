package com.resumerocket.common;

import java.util.Map;

/** Standard error shape returned by every endpoint, per contracts/rest-api.md. */
public record ErrorResponse(String error, Map<String, Object> details) {

  public static ErrorResponse of(String error) {
    return new ErrorResponse(error, null);
  }

  public static ErrorResponse of(String error, Map<String, Object> details) {
    return new ErrorResponse(error, details);
  }
}
