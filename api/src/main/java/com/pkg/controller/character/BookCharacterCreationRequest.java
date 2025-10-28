package com.pkg.controller.character;

public record BookCharacterCreationRequest(
        String name,
        String personality,
        String userDescription,
        String appearanceDescription
) {
}
