package com.pkg.core;

import java.util.ArrayList;
import java.util.List;

public class LLMChain {

    private final List<LLMStep> llmSteps;

    public LLMChain() {
        this.llmSteps = new ArrayList<>();
    }

    private LLMChain(List<LLMStep> llmSteps) {
        this.llmSteps = llmSteps;
    }

    public String operate(String input) {
        String beforeInput = input;
        String output = "" ;
        for(LLMStep step : llmSteps) {
            output = step.operate(beforeInput);
            beforeInput = output;
        }
        return output;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private List<LLMStep> builderLLMStep;

        public Builder() {
            this.builderLLMStep = new ArrayList<>();
        }

        public Builder add(LLMStep llmStep) {
            builderLLMStep.add(llmStep);
            return this;
        }

        public LLMChain build() {
            return new LLMChain(new ArrayList<>(builderLLMStep));
        }
    }
}
