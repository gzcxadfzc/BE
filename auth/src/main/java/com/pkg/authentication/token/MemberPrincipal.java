package com.pkg.authentication.token;

import com.pkg.authentication.core.AuthenticationPrincipal;
import com.pkg.domain.member.Role;

public class MemberPrincipal extends AuthenticationPrincipal {

    private final Long memberId;
    private final Role role;

    public MemberPrincipal(Long id, Role role) {
        super(String.valueOf(id));
        this.memberId = id;
        this.role = role;
    }

    public static MemberPrincipal fromPayload(AccessTokenPayload payload) {
        return new MemberPrincipal(
                payload.memberNo(),
                Role.ofName(payload.role())
        );
    }

    public Long getMemberId() {
        return this.memberId;
    }

    public Role getRole() {
        return role;
    }
}
