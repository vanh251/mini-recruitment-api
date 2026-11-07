package com.example.recruitment.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ApiError> business(ApiException ex, HttpServletRequest request) {
    return error(ex.getStatus(), ex.getCode(), ex.getMessage(), request, Map.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    Map<String, String> fields = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
    return error(
        HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request, fields);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    ConstraintViolationException.class
  })
  ResponseEntity<ApiError> malformed(Exception ex, HttpServletRequest request) {
    return error(
        HttpStatus.BAD_REQUEST,
        "INVALID_REQUEST",
        "Malformed JSON or invalid request parameter",
        request,
        Map.of());
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ResponseEntity<ApiError> methodValidation(
      HandlerMethodValidationException ex, HttpServletRequest request) {
    Map<String, String> fields = new LinkedHashMap<>();
    ex.getParameterValidationResults()
        .forEach(
            result -> {
              if (result instanceof ParameterErrors errors) {
                errors
                    .getFieldErrors()
                    .forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
              } else {
                String name = result.getMethodParameter().getParameterName();
                result
                    .getResolvableErrors()
                    .forEach(
                        e ->
                            fields.putIfAbsent(
                                name == null ? "parameter" : name,
                                e.getDefaultMessage() == null
                                    ? "Invalid value"
                                    : e.getDefaultMessage()));
              }
            });
    return error(
        HttpStatus.valueOf(ex.getStatusCode().value()),
        "VALIDATION_ERROR",
        "Request validation failed",
        request,
        fields);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiError> integrity(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    return error(
        HttpStatus.CONFLICT,
        "DATA_CONFLICT",
        "Request conflicts with existing data",
        request,
        Map.of());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ApiError> missing(NoResourceFoundException ex, HttpServletRequest request) {
    return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", request, Map.of());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest request) {
    if (ex instanceof ErrorResponse protocolError) {
      HttpStatus status = HttpStatus.valueOf(protocolError.getStatusCode().value());
      ApiError body =
          new ApiError(
              Instant.now(),
              status.value(),
              "HTTP_ERROR",
              status.getReasonPhrase(),
              request.getRequestURI(),
              Map.of());
      return ResponseEntity.status(status).headers(protocolError.getHeaders()).body(body);
    }
    LOG.error("Unhandled request error", ex);
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "An unexpected error occurred",
        request,
        Map.of());
  }

  private ResponseEntity<ApiError> error(
      HttpStatus status,
      String code,
      String message,
      HttpServletRequest request,
      Map<String, String> fields) {
    return ResponseEntity.status(status)
        .body(
            new ApiError(
                Instant.now(), status.value(), code, message, request.getRequestURI(), fields));
  }
}
