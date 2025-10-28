package com.pkg.domain.image;

import com.pkg.domain.exception.DomainException;
import com.pkg.domain.exception.DomainExceptionCode;

public class ImageException extends DomainException {

    protected ImageException(DomainExceptionCode code, String message) {
        super(code, message);
    }

    public static ImageException uploadFailed(String message) {
        return new ImageException(DomainExceptionCode.E500, "upload failed :" + message);
    }
}
