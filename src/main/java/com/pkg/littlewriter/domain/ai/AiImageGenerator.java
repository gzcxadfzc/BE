package com.pkg.littlewriter.domain.ai;

import com.pkg.littlewriter.domain.ai.input.GenerateImageInputDto;
import com.pkg.littlewriter.domain.ai.modelBehavior.GenerateIllustrationBehavior;
import com.pkg.littlewriter.domain.ai.response.GenerateImageResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiImageGenerator extends Ai<GenerateImageInputDto, GenerateImageResponseDto> {
    @Autowired
    public AiImageGenerator(GenerateIllustrationBehavior generateIllustrationBehavior) {
        super(generateIllustrationBehavior);
    }
}
