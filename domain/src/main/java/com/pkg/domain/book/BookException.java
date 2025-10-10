package com.pkg.domain.book;

import com.pkg.domain.exception.DomainException;
import com.pkg.domain.exception.ExceptionCode;

public class BookException extends DomainException {

    protected BookException(ExceptionCode code, String message) {
        super(code, message);
    }

    public static BookException bookNotFoundException(String bookId) {
        return new BookException(
                ExceptionCode.E404,
                bookId + " book not found."
        );
    }

    public static BookException emptyBookException(String memberId) {
        return new BookException(
                ExceptionCode.E404,
                "member: " + memberId + " book not found."
        );
    }
}
