package com.pkg.controller.bookprogress;

import com.pkg.domain.bookprogress.BookPagePollResult;
import com.pkg.domain.bookprogress.BookPageResult;

import java.util.List;

public record BookPageStatusResponse(
        String status,
        PageData page
) {
    public record PageData(
            int pageIndex,
            String context,
            String imageUrl,
            List<String> questions
    ) {
        static PageData from(BookPageResult result) {
            return new PageData(
                    result.pageIndex(),
                    result.context(),
                    result.imageUrl(),
                    result.questions()
            );
        }
    }

    public static BookPageStatusResponse from(BookPagePollResult pollResult) {
        if (pollResult.page() == null) {
            return new BookPageStatusResponse(pollResult.status(), null);
        }
        return new BookPageStatusResponse(pollResult.status(), PageData.from(pollResult.page()));
    }
}
