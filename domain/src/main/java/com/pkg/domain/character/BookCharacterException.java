package com.pkg.domain.character;

import com.pkg.domain.bookprogress.BookProgressException;
import com.pkg.domain.exception.DomainException;
import com.pkg.domain.exception.ExceptionCode;

public class BookCharacterException extends DomainException {

    protected BookCharacterException(ExceptionCode code, String message) {
        super(code, message);
    }

    public static BookProgressException notFound(String id) {
        return new BookProgressException(
                ExceptionCode.E404,
                id + "not found"
        );
    }

    public static BookProgressException forbidden() {
        return new BookProgressException(
                ExceptionCode.E403,
                "not authorized resource"
        );
    }
}
