package com.pkg.domain.bookprogress;

import com.pkg.domain.exception.DomainException;
import com.pkg.domain.exception.DomainExceptionCode;

public class BookProgressException extends DomainException {

    public BookProgressException(DomainExceptionCode code, String message) {
        super(code, message);
    }

    public static BookProgressException notFound(String resource) {
        return new BookProgressException(
                DomainExceptionCode.E404,
                resource + " not found."
        );
    }

    public static BookProgressException forbiddenResource() {
        return new BookProgressException(
                DomainExceptionCode.E403,
                "not authorized"
        );
    }
}
