package com.pkg.littlewriter.domain.bookProgressHelper.exceptions;

public class ContextAndQuestionCreationFailedException extends BookProgressException {
    public ContextAndQuestionCreationFailedException(String message) {
        super(message + "failed during create context and question");
    }
}
