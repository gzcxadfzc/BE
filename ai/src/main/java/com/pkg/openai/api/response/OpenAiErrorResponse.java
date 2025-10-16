package com.pkg.openai.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenAiErrorResponse(
        @JsonProperty("event_id") String eventId,
        String type,
        Error error
) {
    public record Error(
            String type,
            String code,
            String message
    ) {
    }
}
