package com.pkg.littlewriter.domain.refactor.ai.generator;

import com.pkg.littlewriter.domain.refactor.ai.input.AiTextInput;
import com.pkg.littlewriter.domain.refactor.ai.modelBehavior.GenerateIllustrationBehavior;
import com.pkg.littlewriter.domain.refactor.ai.response.AiImageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiImageGenerator extends Ai<AiTextInput, AiImageResponse> {
    @Autowired
    public AiImageGenerator(GenerateIllustrationBehavior generateIllustrationBehavior) {
        super(generateIllustrationBehavior);
    }
}
