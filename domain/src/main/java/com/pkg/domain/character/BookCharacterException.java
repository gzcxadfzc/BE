package com.pkg.domain.character;

import com.pkg.domain.bookprogress.BookProgressException;
import com.pkg.domain.exception.DomainException;
import com.pkg.domain.exception.DomainExceptionCode;

public class BookCharacterException extends DomainException {

    protected BookCharacterException(DomainExceptionCode code, String message) {
        super(code, message);
    }

    public static BookProgressException notFound(String id) {
        return new BookProgressException(
                DomainExceptionCode.E404,
                id + "not found"
        );
    }

    public static BookProgressException forbidden() {
        return new BookProgressException(
                DomainExceptionCode.E403,
                "not authorized resource"
        );
    }
}
