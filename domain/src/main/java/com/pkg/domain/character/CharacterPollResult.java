package com.pkg.domain.character;

public record CharacterPollResult(String status) {

    public static CharacterPollResult pending() {
        return new CharacterPollResult("PENDING");
    }

    public static CharacterPollResult ready() {
        return new CharacterPollResult("READY");
    }
}
