package com.pkg.openai.api.common;

import com.fasterxml.jackson.annotation.JsonValue;
import com.pkg.openai.api.exception.OpenAiInternalException;

import java.util.Arrays;

public enum OpenAiModel {

    GPT_4_O_MINI("gpt-4o-mini"),
    GPT_4_1_NANO("gpt-4.1-nano"),
    GPT_4_1_MINI("gpt-4.1-mini")
    ;

    private final String value;

    OpenAiModel(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static OpenAiModel findByValue(String target) {
        return Arrays.stream(values())
                .filter(model -> model.value.equals(target))
                .findFirst()
                .orElseThrow(() -> new OpenAiInternalException("no such model username:" + target));
    }
}
