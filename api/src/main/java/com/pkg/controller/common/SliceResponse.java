package com.pkg.controller.common;

import com.pkg.domain.common.SliceResult;

import java.util.List;

public record SliceResponse<T>(
        List<T> elements,
        int index,
        boolean hasNext
) {
    public static <T> SliceResponse<T> from(SliceResult<T> result) {
        return new SliceResponse<>(result.getElements(), result.getIndex(), result.isHasNext());
    }
}
