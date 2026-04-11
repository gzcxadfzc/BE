package com.pkg.domain.bookprogress;

import java.util.List;

public record BookPageResult(
        int pageIndex,
        String context,
        String imageUrl,
        List<String> questions
) {}
