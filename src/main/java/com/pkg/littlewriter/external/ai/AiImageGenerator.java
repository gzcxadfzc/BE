package com.pkg.littlewriter.external.ai;

import com.pkg.littlewriter.external.ai.exceptions.AiException;
import com.pkg.littlewriter.external.ai.response.GenerateImageResponseDto;
import com.pkg.littlewriter.external.ai.input.GenerateImageInputDto;
import com.pkg.littlewriter.external.ai.modelBehavior.GenerateIllustrationClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiImageGenerator extends Ai<GenerateImageInputDto, GenerateImageResponseDto> {
    @Autowired
    public AiImageGenerator(GenerateIllustrationClient generateIllustrationClient) {
        super(generateIllustrationClient);
    }

    @Override
    public GenerateImageResponseDto getResponseFrom(GenerateImageInputDto generateImageInputDto) throws AiException {
        String rawResponse = aiApiClient.getResponseFrom(generateImageInputDto);
        return new GenerateImageResponseDto(rawResponse);
    }
}
