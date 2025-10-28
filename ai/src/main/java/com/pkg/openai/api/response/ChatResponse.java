package com.pkg.openai.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pkg.openai.api.common.OpenAiResponseStatus;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatResponse(
        String id,
        String object,
        @JsonProperty("created_at") long createdAt,
        OpenAiResponseStatus status,
        String error,
        @JsonProperty("incompleted_details") String incompletedDetails,
        String instructions,
        @JsonProperty("max_output_tokens") String maxOutputTokens,
        String model,
        List<Output> output,
        Usage usage
) {
    public String getFirstContent() {
        return output.getFirst().content.getFirst().text;
    }

    public record Output(
            String type,
            String id,
            String status,
            String role,
            List<Content> content
    ) {

        public record Content(
                String type,
                String text,
                List<Object> annotations
        ) {
        }
    }

    public record Usage(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("output_tokens") int outputTokens,
            @JsonProperty("total_tokens") int totalTokens
    ) {
    }
}
