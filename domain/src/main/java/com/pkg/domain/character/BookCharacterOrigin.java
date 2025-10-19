package com.pkg.domain.character;

import com.pkg.domain.member.Actor;

public record BookCharacterOrigin(
    byte[] imageBytes,
    String name,
    String personality,
    Actor owner
) {
}
