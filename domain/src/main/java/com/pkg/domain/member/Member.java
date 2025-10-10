package com.pkg.domain.member;

public record Member(
    String id,
    String name,
    Role role
) {
}
