package com.pkg.domain.common;

public record PageInfo(
        int index,
        int totalPages,
        long totalElements,
        boolean isLast
) {
}
