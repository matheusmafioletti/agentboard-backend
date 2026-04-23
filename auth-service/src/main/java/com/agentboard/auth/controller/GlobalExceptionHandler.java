package com.agentboard.auth.controller;

import com.agentboard.auth.exception.DuplicateEmailException;
import com.agentboard.auth.exception.InvalidCredentialsException;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain exceptions to HTTP error responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Maps {@link DuplicateEmailException} to HTTP 409.
   */
  @ExceptionHandler(DuplicateEmailException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public Map<String, Object> handleDuplicateEmail(DuplicateEmailException ex) {
    return errorBody("EMAIL_ALREADY_REGISTERED", ex.getMessage());
  }

  /**
   * Maps {@link InvalidCredentialsException} to HTTP 401.
   */
  @ExceptionHandler(InvalidCredentialsException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public Map<String, Object> handleInvalidCredentials(InvalidCredentialsException ex) {
    return errorBody("INVALID_CREDENTIALS", ex.getMessage());
  }

  /**
   * Maps bean validation failures to HTTP 400 with a summary of the first violation.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getAllErrors().stream()
        .findFirst()
        .map(err -> err instanceof FieldError fe
            ? fe.getField() + ": " + fe.getDefaultMessage()
            : err.getDefaultMessage())
        .orElse("Validation failed");
    return errorBody("VALIDATION_ERROR", message);
  }

  private static Map<String, Object> errorBody(String error, String message) {
    return Map.of("error", error, "message", message, "timestamp", OffsetDateTime.now().toString());
  }
}
