package com.pkg.littlewriter.domain.external.ai.modelBehavior;

import com.pkg.littlewriter.domain.external.ai.exceptions.AiException;

@FunctionalInterface
public interface AiModelBehavior <I, R>{
    R getResponseFrom(I aiInput) throws AiException;
}
