package com.pkg.openai.api.exception;

public abstract class OpenAiException extends RuntimeException {

    protected OpenAiException(String message) {
        super(message);
    }
}
