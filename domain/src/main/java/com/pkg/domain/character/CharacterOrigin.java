package com.pkg.domain.character;

public record CharacterOrigin(
    byte[] imageBytes,
    String name,
    String personality,
    String userId
) {
}
