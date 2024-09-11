package com.pkg.littlewriter.domain.refactor.ai.response;

import com.pkg.littlewriter.domain.refactor.ai.AiDataType;

public abstract class AiResponse {
    protected AiDataType type;

    protected AiResponse(AiDataType type) {
        this.type = type;
    }
}
