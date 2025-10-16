package com.pkg.openai.api.exception;

public class OpenAiApiException extends OpenAiException {

    private final int code;

    public OpenAiApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
