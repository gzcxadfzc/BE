package com.pkg.openai.api.request;

import com.pkg.openai.api.common.ImageModelParams.Dalle2Options;
import com.pkg.openai.api.exception.OpenAiInternalException;

public class DallE2RequestBuilder extends ImageRequestBuilder{

    public DallE2RequestBuilder() {
        super.model = Dalle2Options.MODEL;
        super.n = 1;
        super.background = null;
    }

    public DallE2RequestBuilder size256() {
        super.size = Dalle2Options._256;
        return this;
    }

    public DallE2RequestBuilder size512() {
        super.size = Dalle2Options._512;
        return this;
    }

    public DallE2RequestBuilder size1024() {
        super.size = Dalle2Options._1024;
        return this;
    }

    public DallE2RequestBuilder n(int n) {
        if(n < 0 || n > 10) {
            throw new OpenAiInternalException(n + "는 illegal n값입니다." );
        }
        super.n = n;
        return this;
    }

    public DallE2RequestBuilder prompt(String prompt) {
        super.prompt = prompt;
        return this;
    }
}
