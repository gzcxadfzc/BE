package com.pkg.openai.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ImageResponse(
        @JsonProperty("created") long created,
        @JsonProperty("data") List<Data> data,
        @JsonProperty("background") String background,
        @JsonProperty("output_format") String outputFormat,
        @JsonProperty("size") String size,
        @JsonProperty("quality") String quality,
        @JsonProperty("usage") Usage usage
) {

    public record Data(
            @JsonProperty("b64_json") String b64json,
            @JsonProperty("revised_prompt") String revisedPrompt,
            String url
    ) {
    }

    public record Usage(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("output_tokens") int outputTokens,
            @JsonProperty("total_tokens") int totalTokens,
            @JsonProperty("input_tokens_details") InputTokenDetail inputTokenDetail
    ) {

        public record InputTokenDetail(
                @JsonProperty("text_tokens") int textTokens,
                @JsonProperty("image_tokens") int imageTokens
        ) {
        }
    }
}
