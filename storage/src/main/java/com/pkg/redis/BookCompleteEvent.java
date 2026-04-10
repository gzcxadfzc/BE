package com.pkg.redis;

public record BookCompleteEvent(
        String bookInProgressId
) {
}
