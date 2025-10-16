package com.pkg.openai.api.request;

import com.pkg.openai.api.common.ImageModel;
import com.pkg.openai.api.common.ImageModelParams.*;
import com.pkg.openai.api.common.ImageQuality;
import com.pkg.openai.api.common.ImageSize;

public abstract class ImageRequestBuilder {

    protected ImageModel model;
    protected String prompt;
    protected int n;
    protected GptImage1Options.BackgroundOption background;
    protected ImageSize size;
    protected ImageQuality quality;

    public ImageRequest build() {
        return new ImageRequest(
                this.model,
                this.prompt,
                this.n,
                this.background,
                this.size,
                this.quality
        );
    }
}
