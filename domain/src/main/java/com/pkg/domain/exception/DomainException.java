package com.pkg.domain.exception;

public abstract class DomainException extends RuntimeException {

    private ExceptionCode code;

    protected DomainException(ExceptionCode code, String message) {
        super(message);
        this.code = code;
    }

    public ExceptionCode getCode() {
        return code;
    }
}
