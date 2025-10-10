package com.pkg.domain.member;

import com.pkg.domain.exception.DomainException;
import com.pkg.domain.exception.ExceptionCode;

public class MemberException extends DomainException {

    public MemberException(ExceptionCode code, String message) {
        super(code, message);
    }

    public static MemberException memberNotFoundException(String id) {
        return new MemberException(
                ExceptionCode.E404,
                "member " + id + "not found"
        );
    }
}
