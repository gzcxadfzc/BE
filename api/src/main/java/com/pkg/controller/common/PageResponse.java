package com.pkg.controller.common;

import com.pkg.domain.common.PageResult;

import java.util.List;

public record PageResponse<T>(
    List<T> elements,
    int index,
    int totalPages,
    long totalElements,
    boolean isLast
) {

    public static <T> PageResponse<T> from(PageResult<T> result) {
        return new PageResponse<>(
                result.getElements(),
                result.getPageInfo().index(),
                result.getPageInfo().totalPages(),
                result.getPageInfo().totalElements(),
                result.getPageInfo().isLast()
        );
    }
}
