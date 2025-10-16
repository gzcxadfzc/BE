package com.pkg.openai.api.common;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum OpenAiResponseStatus {
    QUEUED("queued"),
    COMPLETED("completed"),
    FAILED("failed"),
    IN_PROGRESS("in_progress"),
    CANCELLED("cancelled"),
    INCOMPLETE("incomplete")
    ;

    @JsonValue
    private final String value;

    OpenAiResponseStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static OpenAiResponseStatus findByValue(String target) {
        return Arrays.stream(values())
                .filter(status -> status.value.equals(target))
                .findFirst()
                .orElseThrow();
    }
}
