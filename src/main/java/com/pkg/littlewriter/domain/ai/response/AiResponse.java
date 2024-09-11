package com.pkg.littlewriter.domain.ai.response;

import com.pkg.littlewriter.domain.ai.commons.AiDataType;

public abstract class AiResponse {
    protected AiDataType type;

    protected AiResponse(AiDataType type) {
        this.type = type;
    }
}
