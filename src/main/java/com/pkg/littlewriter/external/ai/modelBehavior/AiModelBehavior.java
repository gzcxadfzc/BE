package com.pkg.littlewriter.external.ai.modelBehavior;

import com.pkg.littlewriter.external.ai.exceptions.AiException;

@FunctionalInterface
public interface AiModelBehavior <I, R>{
    R getResponseFrom(I aiInput) throws AiException;
}
