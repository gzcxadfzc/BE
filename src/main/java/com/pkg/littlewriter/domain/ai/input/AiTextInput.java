package com.pkg.littlewriter.domain.ai.input;

import com.pkg.littlewriter.domain.ai.commons.AiDataType;
import lombok.Getter;

@Getter
public class AiTextInput extends AiInput {
    private final String inputText;

    public AiTextInput(String inputText) {
        super(AiDataType.TEXT);
        this.inputText = inputText;
    }
}
