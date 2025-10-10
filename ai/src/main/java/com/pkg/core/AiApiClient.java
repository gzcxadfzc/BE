package com.pkg.core;

import com.pkg.core.exceptions.AiException;

@FunctionalInterface
public interface AiApiClient<I>{
    String getResponseFrom(I aiInput) throws AiException;
}
