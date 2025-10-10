package com.pkg.domain.member;

import java.util.Map;

public enum Role {

    GUEST,
    MEMBER,
    ADMIN,
    ;

    private static final Map<String, Role> ROLE_NAME_MAP = Map.of(
            "GUEST", Role.GUEST,
            "MEMBER", Role.MEMBER,
            "ADMIN", Role.ADMIN
    );

    public static Role ofName(String roleName) {
        Role role = ROLE_NAME_MAP.get(roleName);
        if(role == null) {
            throw RoleException.invalidRoleNameException(roleName);
        }
        return role;
    }
}
