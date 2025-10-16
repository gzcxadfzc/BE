package com.pkg.openai.api.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ImageSize {

    _512_512("512x512"),
    _256_256("256x256"),
    _1024_1024("1024x1024"),
    _1536_1024("1536x1024"),
    _1024_1536("1024x1536"),
    _1792_1024("1792x1024"),
    _1024_1792("1024x1792"),
    ;

    private final String value;

    ImageSize(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
