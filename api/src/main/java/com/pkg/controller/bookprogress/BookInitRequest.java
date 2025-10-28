package com.pkg.controller.bookprogress;

public record BookInitRequest(
        String backgroundInfo,
        Long characterId,
        String userInput
) {
}
