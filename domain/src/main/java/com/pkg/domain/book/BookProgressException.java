package com.pkg.domain.book;

import com.pkg.domain.exception.DomainException;
import com.pkg.domain.exception.ExceptionCode;

public class BookProgressException extends DomainException {

    public BookProgressException(ExceptionCode code, String message) {
        super(code, message);
    }

    public static BookProgressException bookInProgressNotFoundException(String id) {
        return new BookProgressException(
                ExceptionCode.E404,
                id + " not found."
        );
    }
}
