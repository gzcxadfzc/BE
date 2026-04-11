package com.pkg.controller.bookprogress;

import com.pkg.domain.bookprogress.BookPageAccepted;

public record BookPageAcceptedResponse(String bipId) {

    public static BookPageAcceptedResponse from(BookPageAccepted accepted) {
        return new BookPageAcceptedResponse(accepted.bipId());
    }
}
