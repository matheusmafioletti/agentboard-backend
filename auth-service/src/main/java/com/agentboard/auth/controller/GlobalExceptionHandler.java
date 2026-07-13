package com.agentboard.auth.controller;

import com.agentboard.auth.exception.AlreadyMemberException;
import com.agentboard.auth.exception.DuplicateEmailException;
import com.agentboard.auth.exception.DuplicatePendingInviteException;
import com.agentboard.auth.exception.DuplicateTenantNameException;
import com.agentboard.auth.exception.ForbiddenOperationException;
import com.agentboard.auth.exception.InvalidCredentialsException;
import com.agentboard.auth.exception.InviteGoneException;
import com.agentboard.auth.exception.LastAdminException;
import com.agentboard.auth.exception.NoMembershipException;
import com.agentboard.auth.exception.NotMemberException;
import com.agentboard.commons.exceptions.DataSourceNotAllowedException;
import com.agentboard.commons.exceptions.InvalidDataSourceException;
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

  /** Maps {@link DuplicateEmailException} to HTTP 409. */
  @ExceptionHandler(DuplicateEmailException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public Map<String, Object> handleDuplicateEmail(DuplicateEmailException ex) {
    return errorBody("EMAIL_ALREADY_REGISTERED", ex.getMessage());
  }

  /** Maps {@link DuplicateTenantNameException} to HTTP 409. */
  @ExceptionHandler(DuplicateTenantNameException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public Map<String, Object> handleDuplicateTenant(DuplicateTenantNameException ex) {
    return errorBody("TENANT_NAME_TAKEN", ex.getMessage());
  }

  /** Maps {@link DuplicatePendingInviteException} to HTTP 409. */
  @ExceptionHandler({DuplicatePendingInviteException.class, AlreadyMemberException.class})
  @ResponseStatus(HttpStatus.CONFLICT)
  public Map<String, Object> handleInviteConflict(RuntimeException ex) {
    return errorBody("CONFLICT", ex.getMessage());
  }

  /** Maps {@link LastAdminException} to HTTP 409. */
  @ExceptionHandler(LastAdminException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public Map<String, Object> handleLastAdmin(LastAdminException ex) {
    return errorBody("LAST_ADMIN", ex.getMessage());
  }

  /** Maps {@link InvalidCredentialsException} to HTTP 401. */
  @ExceptionHandler(InvalidCredentialsException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public Map<String, Object> handleInvalidCredentials(InvalidCredentialsException ex) {
    return errorBody("INVALID_CREDENTIALS", ex.getMessage());
  }

  /** Maps membership and permission errors to HTTP 403. */
  @ExceptionHandler({
      NoMembershipException.class,
      NotMemberException.class,
      ForbiddenOperationException.class
  })
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public Map<String, Object> handleForbidden(RuntimeException ex) {
    return errorBody("FORBIDDEN", ex.getMessage());
  }

  /** Maps expired or invalid invites to HTTP 410. */
  @ExceptionHandler(InviteGoneException.class)
  @ResponseStatus(HttpStatus.GONE)
  public Map<String, Object> handleInviteGone(InviteGoneException ex) {
    return errorBody("INVITE_GONE", ex.getMessage());
  }

  /** Maps invalid {@code X-Data-Source} values to HTTP 400. */
  @ExceptionHandler(InvalidDataSourceException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleInvalidDataSource(InvalidDataSourceException ex) {
    return errorBody("INVALID_DATA_SOURCE", ex.getMessage());
  }

  /** Maps synthetic data source violations to HTTP 403. */
  @ExceptionHandler(DataSourceNotAllowedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public Map<String, Object> handleDataSourceNotAllowed(DataSourceNotAllowedException ex) {
    return errorBody("DATA_SOURCE_NOT_ALLOWED", ex.getMessage());
  }

  /** Maps password mismatch and other argument errors to HTTP 400. */
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleIllegalArgument(IllegalArgumentException ex) {
    return errorBody("BAD_REQUEST", ex.getMessage());
  }

  /** Maps bean validation failures to HTTP 400 with a summary of the first violation. */
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
