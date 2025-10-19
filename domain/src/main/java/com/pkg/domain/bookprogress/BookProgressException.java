package com.pkg.domain.bookprogress;

import com.pkg.domain.exception.DomainException;
import com.pkg.domain.exception.ExceptionCode;

public class BookProgressException extends DomainException {

    public BookProgressException(ExceptionCode code, String message) {
        super(code, message);
    }

    public static BookProgressException notFound(String id) {
        return new BookProgressException(
                ExceptionCode.E404,
                id + " not found."
        );
    }

    public static BookProgressException forbiddenResource() {
        return new BookProgressException(
                ExceptionCode.E403,
                "not authorized"
        );
    }
}
