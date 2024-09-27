package com.pkg.littlewriter.external.ai;

import com.pkg.littlewriter.external.ai.exceptions.AiException;
import com.pkg.littlewriter.external.ai.modelBehavior.AiApiClient;

public abstract class Ai<I, R> {
    protected AiApiClient<I> aiApiClient;

    protected Ai(AiApiClient<I> aiApiClient) {
        this.aiApiClient = aiApiClient;
    }

    public abstract R getResponseFrom(I aiInput) throws AiException;
}
