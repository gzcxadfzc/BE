package com.pkg.littlewriter.domain.refactor.ai.generator;


import com.pkg.littlewriter.domain.refactor.ai.input.AiInput;
import com.pkg.littlewriter.domain.refactor.ai.modelBehavior.AiModelBehavior;
import com.pkg.littlewriter.domain.refactor.ai.response.AiResponse;
import com.pkg.littlewriter.domain.refactor.ai.exceptions.AiException;

public abstract class Ai<I extends AiInput, R extends AiResponse> {
    protected AiModelBehavior<I, R> aiModelBehavior;

    protected Ai(AiModelBehavior<I, R> aiModelBehavior) {
        this.aiModelBehavior = aiModelBehavior;
    }

    public R getResponseFrom(I aiInput) throws AiException {
        return aiModelBehavior.getResponseFrom(aiInput);
    }
}
