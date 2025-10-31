package com.pkg.controller.character;

import com.pkg.domain.character.BookCharacterGenerateRequest;
import com.pkg.domain.member.Actor;
import jakarta.validation.constraints.NotBlank;


public record BookCharacterCreationRequest(
        @NotBlank String name,
        @NotBlank String personality,
        @NotBlank String userDescription,
        @NotBlank String appearanceDescription
) {

    public BookCharacterGenerateRequest toGenerateRequest(Actor actor) {
        return new BookCharacterGenerateRequest(
                actor,
                name(),
                appearanceDescription(),
                personality(),
                userDescription()
        );
    }
}
