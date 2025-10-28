package com.pkg.openai.api.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkg.openai.api.exception.OpenAiInternalException;

public record TextFormat(
        Object format
) {

    public static final ObjectMapper mapper = new ObjectMapper();

    public static TextFormat from(JsonSchema jsonSchema) {
        try {
            return new TextFormat(jsonSchema);
        } catch (Exception e) {
            throw new OpenAiInternalException("could not map jsonSchema to TextFormat");
        }
    }
}
