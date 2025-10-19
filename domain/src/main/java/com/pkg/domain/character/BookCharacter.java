package com.pkg.domain.character;

import com.pkg.domain.member.Actor;

public record BookCharacter(
        String id,
        Actor owner,
        String name,
        String appearanceKeywords,
        String personality,
        String description,
        String imageUrl
) {
}
