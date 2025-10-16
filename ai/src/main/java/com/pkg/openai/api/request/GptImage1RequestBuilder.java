package com.pkg.openai.api.request;

import com.pkg.openai.api.common.ImageModelParams.GptImage1Options;

public class GptImage1RequestBuilder extends ImageRequestBuilder {

    public GptImage1RequestBuilder() {
        super.model = GptImage1Options.MODEL;
        super.n = 1;
    }

    public GptImage1RequestBuilder size1024() {
        super.size = GptImage1Options._1024;
        return this;
    }

    public GptImage1RequestBuilder sizeLandScape() {
        super.size = GptImage1Options.LANDSCAPE;
        return this;
    }

    public GptImage1RequestBuilder sizePortrait() {
        super.size = GptImage1Options.PORTRAIT;
        return this;
    }

    public GptImage1RequestBuilder highQuality() {
        super.quality = GptImage1Options.HIGH_QUALITY;
        return this;
    }

    public GptImage1RequestBuilder mediumQuality() {
        super.quality = GptImage1Options.MEDIUM_QUALITY;
        return this;
    }

    public GptImage1RequestBuilder lowQuality() {
        super.quality = GptImage1Options.LOW_QUALITY;
        return this;
    }

    public GptImage1RequestBuilder prompt(String prompt) {
        super.prompt = prompt;
        return this;
    }
}
