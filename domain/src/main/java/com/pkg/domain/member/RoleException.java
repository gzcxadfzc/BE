package com.pkg.domain.member;

import com.pkg.domain.exception.DomainException;
import com.pkg.domain.exception.DomainExceptionCode;

public class RoleException extends DomainException {

    public RoleException(DomainExceptionCode code, String message) {
        super(code, message);
    }

    public static RoleException invalidRoleNameException(String roleName) {
        return new RoleException(
                DomainExceptionCode.E401,
                roleName + " is not valid role"
        );
    }
}
