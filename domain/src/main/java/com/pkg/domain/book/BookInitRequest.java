package com.pkg.domain.book;

public record BookInitRequest(
        Character character,
        String background,
        String userId
) {
}
