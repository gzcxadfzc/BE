package com.pkg.littlewriter.external.ai;

import com.pkg.littlewriter.external.ai.exceptions.AiException;
import com.pkg.littlewriter.external.ai.response.GenerateImageResponseDto;
import com.pkg.littlewriter.external.ai.input.GenerateImageInputDto;
import com.pkg.littlewriter.external.ai.modelBehavior.GenerateIllustrationBehavior;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiImageGenerator extends Ai<GenerateImageInputDto, GenerateImageResponseDto> {
    @Autowired
    public AiImageGenerator(GenerateIllustrationBehavior generateIllustrationBehavior) {
        super(generateIllustrationBehavior);
    }

    @Override
    public GenerateImageResponseDto getResponseFrom(GenerateImageInputDto generateImageInputDto) throws AiException {
        return aiModelBehavior.getResponseFrom(generateImageInputDto);
    }
}
