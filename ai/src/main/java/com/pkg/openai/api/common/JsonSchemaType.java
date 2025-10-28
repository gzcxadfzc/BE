package com.pkg.openai.api.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum JsonSchemaType {

    STRING("string"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    INTEGER("integer"),
    OBJECT("object"),
    ARRAY("array"),
    ENUM("enum"),
    ANY_OF("anyof");

    private final String value;

    JsonSchemaType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }
}
