package com.pkg.controller.bookprogress;

import jakarta.validation.constraints.NotBlank;

public record BookCompleteRequest(
        @NotBlank String title,
        @NotBlank String author
) {
}
