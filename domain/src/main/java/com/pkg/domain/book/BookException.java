package com.pkg.domain.book;

import com.pkg.domain.bookprogress.BookInProgress;
import com.pkg.domain.exception.DomainException;
import com.pkg.domain.exception.DomainExceptionCode;

public class BookException extends DomainException {

    protected BookException(DomainExceptionCode code, String message) {
        super(code, message);
    }

    public static BookException notFound(String bookId) {
        return new BookException(
                DomainExceptionCode.E404,
                bookId + " book not found."
        );
    }

    public static BookException notAuthorizedBookCreationFrom(BookInProgress bookInProgress) {
        return new BookException(
                DomainExceptionCode.E403,
                "not authorized bookInProgress : " + bookInProgress.id()
        );
    }
}
