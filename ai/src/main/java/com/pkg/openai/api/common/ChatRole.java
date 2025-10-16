package com.pkg.openai.api.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ChatRole {

    DEVELOPER("developer"),
    USER("user"),
    ASSISTANT("assistant"),
    ;

    private final String value;

    ChatRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
