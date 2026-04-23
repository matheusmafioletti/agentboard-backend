package com.agentboard.board.controller;

import com.agentboard.commons.exceptions.ResourceNotFoundException;
import com.agentboard.commons.exceptions.TenantMismatchException;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain exceptions to HTTP error responses for board-service. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** Maps {@link ResourceNotFoundException} to HTTP 404. */
  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Map<String, Object> handleNotFound(ResourceNotFoundException ex) {
    return errorBody("RESOURCE_NOT_FOUND", ex.getMessage());
  }

  /** Maps {@link TenantMismatchException} to HTTP 403. */
  @ExceptionHandler(TenantMismatchException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public Map<String, Object> handleTenantMismatch(TenantMismatchException ex) {
    return errorBody("FORBIDDEN", ex.getMessage());
  }

  /** Maps illegal-state errors (e.g. task already completed) to HTTP 409. */
  @ExceptionHandler(IllegalStateException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public Map<String, Object> handleConflict(IllegalStateException ex) {
    return errorBody("CONFLICT", ex.getMessage());
  }

  /** Maps bean validation failures to HTTP 400. */
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
