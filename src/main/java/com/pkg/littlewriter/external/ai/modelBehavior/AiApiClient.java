package com.pkg.littlewriter.external.ai.modelBehavior;

import com.pkg.littlewriter.external.ai.exceptions.AiException;

@FunctionalInterface
public interface AiApiClient<I>{
    String getResponseFrom(I aiInput) throws AiException;
}
