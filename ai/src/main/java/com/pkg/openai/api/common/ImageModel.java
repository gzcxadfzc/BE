package com.pkg.openai.api.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ImageModel {

    DALL_E_2("dall-e-2"),
    DALL_E_3("dall-e-3"),
    GPT_IMAGE_1("gpt-image-1"),
    ;

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    ImageModel(String value) {
        this.value = value;
    }
}
