package com.pkg.openai.api.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkg.openai.api.exception.OpenAiInternalException;

public record JsonSchema(
    String type,
    String name,
    Boolean strict,
    Object schema
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private static final String type = "json_schema";
        private String name;
        private Boolean strict;
        private Object schema;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder strict(Boolean strict) {
            this.strict = strict;
            return this;
        }

        public Builder schema(String schemaJson) {
            try {
                this.schema = new ObjectMapper().readValue(schemaJson, Object.class);
            } catch (JsonProcessingException e) {
                throw new OpenAiInternalException(e.getMessage());
            }
            return this;
        }

        public JsonSchema build() {
            return new JsonSchema(type, name, strict, schema);
        }
    }
}
