package com.pkg.controller.auth;

import jakarta.validation.constraints.NotBlank;

public record SignUpRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
