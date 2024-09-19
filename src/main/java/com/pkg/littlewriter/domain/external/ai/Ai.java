package com.pkg.littlewriter.domain.external.ai;

import com.pkg.littlewriter.domain.external.ai.exceptions.AiException;
import com.pkg.littlewriter.domain.external.ai.modelBehavior.AiModelBehavior;

public abstract class Ai<I, R> {
    protected AiModelBehavior<I, R> aiModelBehavior;

    protected Ai(AiModelBehavior<I, R> aiModelBehavior) {
        this.aiModelBehavior = aiModelBehavior;
    }

//    public R getResponseFrom(I aiInput) throws AiException {
//        return aiModelBehavior.getResponseFrom(aiInput);
//    }
}
