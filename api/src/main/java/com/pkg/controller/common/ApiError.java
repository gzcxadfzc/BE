package com.pkg.controller.common;

public record ApiError(
        String code,
        String message
) {
}
