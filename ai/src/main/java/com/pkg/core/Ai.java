package com.pkg.core;

import com.pkg.core.exceptions.AiException;

public abstract class Ai<I, R> {
    protected AiApiClient<I> aiApiClient;

    protected Ai(AiApiClient<I> aiApiClient) {
        this.aiApiClient = aiApiClient;
    }

    public abstract R getResponseFrom(I aiInput) throws AiException;
}
