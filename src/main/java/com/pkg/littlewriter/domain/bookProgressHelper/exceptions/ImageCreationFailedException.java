package com.pkg.littlewriter.domain.bookProgressHelper.exceptions;

public class ImageCreationFailedException extends BookProgressException {
    ImageCreationFailedException(String message) {
        super(message + "failed during create image");
    }
}
