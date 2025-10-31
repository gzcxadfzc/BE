package com.pkg.controller.auth;

import jakarta.validation.constraints.NotBlank;

public record SignInRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
