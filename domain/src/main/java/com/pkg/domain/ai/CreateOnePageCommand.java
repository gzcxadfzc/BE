package com.pkg.domain.ai;


import com.pkg.domain.member.Actor;

public record CreateOnePageCommand(
        String bipId,
        String userInput,
        Actor currentUser
) {
}
