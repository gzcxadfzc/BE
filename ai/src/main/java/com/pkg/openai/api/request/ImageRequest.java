package com.pkg.openai.api.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pkg.openai.api.common.ImageModel;
import com.pkg.openai.api.common.ImageModelParams.*;
import com.pkg.openai.api.common.ImageQuality;
import com.pkg.openai.api.common.ImageSize;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImageRequest(
        @JsonProperty("model") ImageModel model,
        @JsonProperty("prompt") String prompt,
        @JsonProperty("n") int n,
        @JsonProperty("background") GptImage1Options.BackgroundOption background,
        @JsonProperty("size") ImageSize size,
        @JsonProperty("quality")ImageQuality quality
        ) {

        public static DallE2RequestBuilder dallE2Builder() {
            return new DallE2RequestBuilder();
        }

        public static DallE3RequestBuilder dallE3Builder() {
            return new DallE3RequestBuilder();
        }

        public static GptImage1RequestBuilder gptImage1RequestBuilder() {
            return new GptImage1RequestBuilder();
        }
}
