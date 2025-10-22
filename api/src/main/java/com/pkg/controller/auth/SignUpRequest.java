package com.pkg.controller.auth;

public record SignUpRequest(
        String username,
        String password
) {
}
