package com.swissquote.caa.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps application exceptions to RFC 7807 problem details (see docs/api-contract.md).
 * Extends {@link ResponseEntityExceptionHandler} so framework exceptions (unknown path ->
 * 404, wrong method -> 405, bad media type -> 415, unreadable body -> 400, handler-method
 * validation -> 400, ...) keep their proper client-error status instead of falling into
 * the 500 catch-all.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail handleBadCredentials(BadCredentialsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    ProblemDetail handleTooManyLoginAttempts(TooManyLoginAttemptsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
            "Too many failed login attempts — try again later");
    }

    /** Class-level @Validated constraint violations on request parameters. */
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException e) {
        String detail = e.getConstraintViolations().stream().findFirst()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .orElse("Invalid request parameter");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
            "Invalid value for parameter '" + e.getName() + "'");
    }

    /** Keeps the "field: message" detail promised by the contract for body validation. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        FieldError first = e.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String detail = first == null
            ? "Invalid request"
            : first.getField() + ": " + first.getDefaultMessage();
        return ResponseEntity.status(status)
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status.value()), detail));
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred");
    }
}
