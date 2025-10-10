package com.pkg.domain.member;

public record CurrentUser(
    Long memberId,
    Role role
) {
}
