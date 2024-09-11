package com.pkg.littlewriter.domain.refactor.ai.response;

import com.pkg.littlewriter.domain.refactor.ai.AiDataType;
import lombok.Getter;

@Getter
public class AiImageResponse extends AiResponse {
    private final String imageUrl;

    public AiImageResponse(String imageUrl) {
        super(AiDataType.IMAGE);
        this.imageUrl = imageUrl;
    }
}
