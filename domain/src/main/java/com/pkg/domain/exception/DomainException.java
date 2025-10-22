package com.pkg.domain.exception;

public abstract class DomainException extends RuntimeException {

    private DomainExceptionCode code;

    protected DomainException(DomainExceptionCode code, String message) {
        super(message);
        this.code = code;
    }

    public DomainExceptionCode getCode() {
        return code;
    }
}
