package com.pkg.domain.character;

import com.pkg.domain.member.Actor;

public record BookCharacterCreationRequest(
        String name,
        String appearanceKeyword,
        String imageUrl,
        String personality,
        String userDescription,
        Actor currentUser
) {
}
