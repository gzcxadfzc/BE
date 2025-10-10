package com.pkg.domain.member;

import com.pkg.domain.exception.DomainException;
import com.pkg.domain.exception.ExceptionCode;

public class RoleException extends DomainException {

    public RoleException(ExceptionCode code, String message) {
        super(code, message);
    }

    public static RoleException invalidRoleNameException(String roleName) {
        return new RoleException(
                ExceptionCode.E401,
                roleName + " is not valid role"
        );
    }
}
