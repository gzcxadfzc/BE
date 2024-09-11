package com.pkg.littlewriter.domain.ai;

import com.pkg.littlewriter.domain.ai.response.AiImageResponse;
import com.pkg.littlewriter.domain.ai.input.AiTextInput;
import com.pkg.littlewriter.domain.ai.modelBehavior.GenerateIllustrationBehavior;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiImageGenerator extends Ai<AiTextInput, AiImageResponse> {
    @Autowired
    public AiImageGenerator(GenerateIllustrationBehavior generateIllustrationBehavior) {
        super(generateIllustrationBehavior);
    }
}
