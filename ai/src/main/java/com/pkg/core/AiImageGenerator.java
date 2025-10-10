package com.pkg.core;

import com.pkg.core.exceptions.AiException;
import com.pkg.core.response.GenerateImageResponseDto;
import com.pkg.core.input.GenerateImageInputDto;
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
