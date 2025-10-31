package com.pkg.controller.bookprogress;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookInitRequest(
        @NotBlank String backgroundInfo,
        @NotNull Long characterId,
        @NotBlank String userInput
) {
}
