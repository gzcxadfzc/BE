package com.pkg.authentication.password;

import com.pkg.authentication.core.AuthenticationException;
import com.pkg.authentication.core.AuthenticationExceptionType;

public class UsernamePasswordException extends AuthenticationException {

    public UsernamePasswordException(String message, AuthenticationExceptionType type) {
        super(message, type);
    }

    public static UsernamePasswordException notFound(UsernamePassword usernamePassword) {
        return new UsernamePasswordException(
                "username : " + usernamePassword.getUsername() + " not fouond",
                AuthenticationExceptionType.BAD_CREDENTIAL
        );
    }

    public static UsernamePasswordException wrongPassword(String message) {
        return new UsernamePasswordException(
                "invalid credential : " + message,
                AuthenticationExceptionType.BAD_CREDENTIAL
        );
    }
}
