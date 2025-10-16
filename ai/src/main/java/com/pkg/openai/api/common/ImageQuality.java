package com.pkg.openai.api.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ImageQuality {

    AUTO("auto"),
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low"),
    HD("hd"),
    STANDARD("standard"),
    ;

    private final String value;

    ImageQuality(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
