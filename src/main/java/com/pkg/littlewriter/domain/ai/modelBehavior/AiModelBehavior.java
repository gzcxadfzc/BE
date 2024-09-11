package com.pkg.littlewriter.domain.ai.modelBehavior;

import com.pkg.littlewriter.domain.ai.exceptions.AiException;
import com.pkg.littlewriter.domain.ai.response.AiResponse;
import com.pkg.littlewriter.domain.ai.input.AiInput;

@FunctionalInterface
public interface AiModelBehavior <I extends AiInput, R extends AiResponse>{
    R getResponseFrom(I aiInput) throws AiException;
}
