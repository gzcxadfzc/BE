package com.pkg.authentication.core;

public class AuthenticationException extends RuntimeException {

    private final AuthenticationExceptionType type;

    public AuthenticationException(String message, AuthenticationExceptionType type) {
        super(message);
        this.type = type;
    }

    public AuthenticationExceptionType getType() {
        return type;
    }

    public static AuthenticationException invalidClaimException() {
        return new AuthenticationException(
                "authentication failed: invalid claim",
                AuthenticationExceptionType.INVALID_CLAIM
        );
    }

    public static AuthenticationException missingBearerTokenException(String message) {
        return new AuthenticationException(
                message,
                AuthenticationExceptionType.MISSING_CREDENTIAL
        );
    }

    public static AuthenticationException invalidCredential(String message) {
        return new AuthenticationException(
                message,
                AuthenticationExceptionType.INVALID_CREDENTIAL
        );
    }
}
