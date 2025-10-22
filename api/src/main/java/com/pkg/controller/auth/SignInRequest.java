package com.pkg.controller.auth;

public record SignInRequest(
        String username,
        String password
) {
}
