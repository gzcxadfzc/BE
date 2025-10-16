package com.pkg.openai.api.request;

import com.pkg.openai.api.common.ImageModelParams.Dalle3Options;

public class DallE3RequestBuilder extends ImageRequestBuilder {

    public DallE3RequestBuilder() {
        super.model = Dalle3Options.MODEL;
        super.n = 1;
        super.background = null;
    }

    public DallE3RequestBuilder size1024() {
        super.size = Dalle3Options._1024;
        return this;
    }

    public DallE3RequestBuilder sizeLandScape() {
        super.size = Dalle3Options.LANDSCAPE;
        return this;
    }

    public DallE3RequestBuilder sizePortrait() {
        super.size = Dalle3Options.PORTRAIT;
        return this;
    }

    public DallE3RequestBuilder standardQuality() {
        super.quality = Dalle3Options.STANDARD_QUALITY;
        return this;
    }

    public DallE3RequestBuilder hdQuality() {
        super.quality = Dalle3Options.HD_QUALITY;
        return this;
    }

    public DallE3RequestBuilder prompt(String prompt) {
        super.prompt = prompt;
        return this;
    }
}
