package com.pkg.domain.member;

import com.pkg.domain.exception.DomainException;
import com.pkg.domain.exception.DomainExceptionCode;

public class MemberException extends DomainException {

    public MemberException(DomainExceptionCode code, String message) {
        super(code, message);
    }

    public static MemberException notFound(String username) {
        return new MemberException(
                DomainExceptionCode.E404,
                "username: " + username + " not found"
        );
    }

    public static MemberException invalidPassword() {
        return new MemberException(
                DomainExceptionCode.E404,
                "wrong password"
        );
    }

    public static MemberException duplicatedUsername(String username) {
        return new MemberException(
                DomainExceptionCode.E409,
                username + "is alreadyExists"
        );
    }

    public static MemberException invalidCommand(String message) {
        return new MemberException(
                DomainExceptionCode.E400,
                message
        );
    }
}
