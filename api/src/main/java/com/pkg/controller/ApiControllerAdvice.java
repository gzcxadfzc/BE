package com.pkg.controller;

import com.pkg.authentication.core.AuthenticationException;
import com.pkg.authentication.core.AuthenticationExceptionType;
import com.pkg.controller.common.ApiError;
import com.pkg.domain.exception.DomainException;
import com.pkg.domain.exception.DomainExceptionCode;
import com.pkg.openai.api.exception.OpenAiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(ApiControllerAdvice.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception e) {
        log.error("Unhandled exception: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(e.getMessage(), e.getCause().getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomainException(DomainException e) {
        log.error("Domain Exception: {}", e.getMessage());
        return ResponseEntity.status(DomainCodeMapper.toStatusCode(e))
                .body(new ApiError(e.getCode().name(), e.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleDomainException(AuthenticationException e) {
        log.error("Authentication Exception: {}", e.getMessage());
        return ResponseEntity.status(AuthTypeMapper.toStatusCode(e))
                .body(new ApiError(e.getType().name(), e.getMessage()));
    }

    @ExceptionHandler(OpenAiException.class)
    public ResponseEntity<ApiError> handleDomainException(OpenAiException e) {
        log.error("Open AI Exception: {}", e.getMessage());
        return ResponseEntity.status(500)
                .body(new ApiError("E500", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationError(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .toList();

        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .badRequest()
                .body(new ApiError("E400", "bad request"));
    }

    private static class DomainCodeMapper {

        private static final Map<DomainExceptionCode, HttpStatusCode> map = Map.of(
            DomainExceptionCode.E400, HttpStatusCode.valueOf(400),
            DomainExceptionCode.E401, HttpStatusCode.valueOf(401),
            DomainExceptionCode.E402, HttpStatusCode.valueOf(402),
            DomainExceptionCode.E403, HttpStatusCode.valueOf(403),
            DomainExceptionCode.E404, HttpStatusCode.valueOf(404),
            DomainExceptionCode.E405, HttpStatusCode.valueOf(405),
            DomainExceptionCode.E406, HttpStatusCode.valueOf(406),
            DomainExceptionCode.E407, HttpStatusCode.valueOf(407),
            DomainExceptionCode.E408, HttpStatusCode.valueOf(408),
            DomainExceptionCode.E409, HttpStatusCode.valueOf(409)
        );

        public static HttpStatusCode toStatusCode(DomainException domainException) {
            return map.getOrDefault(domainException.getCode(), HttpStatusCode.valueOf(500));
        }
    }

    private static class AuthTypeMapper {

        private static final Map<AuthenticationExceptionType, HttpStatusCode> map = Map.of(
            AuthenticationExceptionType.EXPIRED_CREDENTIAL, HttpStatusCode.valueOf(401),
            AuthenticationExceptionType.INVALID_CLAIM, HttpStatusCode.valueOf(403),
            AuthenticationExceptionType.BAD_CREDENTIAL, HttpStatusCode.valueOf(401),
            AuthenticationExceptionType.MISSING_CREDENTIAL, HttpStatusCode.valueOf(400),
            AuthenticationExceptionType.INVALID_CREDENTIAL, HttpStatusCode.valueOf(400),
            AuthenticationExceptionType.UNKNOWN, HttpStatusCode.valueOf(403)
        );

        public static HttpStatusCode toStatusCode(AuthenticationException authException) {
            return map.getOrDefault(authException.getType(), HttpStatusCode.valueOf(500));
        }
    }
}


