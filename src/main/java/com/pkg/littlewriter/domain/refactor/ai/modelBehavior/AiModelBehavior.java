package com.pkg.littlewriter.domain.refactor.ai.modelBehavior;

import com.pkg.littlewriter.domain.refactor.ai.response.AiResponse;
import com.pkg.littlewriter.domain.refactor.ai.input.AiInput;
import com.pkg.littlewriter.domain.refactor.ai.exceptions.AiException;

@FunctionalInterface
public interface AiModelBehavior <I extends AiInput, R extends AiResponse>{
    R getResponseFrom(I aiInput) throws AiException;
}
