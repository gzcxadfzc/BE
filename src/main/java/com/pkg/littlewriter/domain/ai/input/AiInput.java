package com.pkg.littlewriter.domain.ai.input;

import com.pkg.littlewriter.domain.ai.commons.AiDataType;
import lombok.Getter;

@Getter
public abstract class AiInput {
    protected AiDataType dataType;

    protected AiInput(AiDataType dataType) {
        this.dataType = dataType;
    }
}
