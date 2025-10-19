package com.pkg.core;

import java.util.function.Function;

public class LLMStep implements CallStep<String, String> {

    private final Function<String, String> llmOperation;
    private final int maxAttempt;

    public LLMStep(Function<String, String> llmOperation, int maxAttempt) {
        this.llmOperation = llmOperation;
        this.maxAttempt = maxAttempt;
    }

    public LLMStep(Function<String, String> llmOperation) {
        this.llmOperation = llmOperation;
        this.maxAttempt = 1;
    }

    @Override
    public String operate(String input) {
        return llmOperation.apply(input);
    }
}
