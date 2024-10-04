package com.pkg.littlewriter.domain.bookProgressHelper.exceptions;

public class ImageCreationFailedException extends BookProgressException {
    public ImageCreationFailedException(String message) {
        super(message + "failed during create image");
    }
}
