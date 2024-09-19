package com.pkg.littlewriter.domain.ai.modelBehavior;

import com.pkg.littlewriter.domain.ai.exceptions.AiException;

@FunctionalInterface
public interface AiModelBehavior <I, R>{
    R getResponseFrom(I aiInput) throws AiException;
}
