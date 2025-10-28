package com.pkg.domain.bookprogress;

import com.pkg.domain.member.Actor;

public record BookInitCommand(
        Long characterId,
        String background,
        Actor currentUser,
        String userInput
) {
}
