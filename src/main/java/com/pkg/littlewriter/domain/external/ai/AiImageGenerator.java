package com.pkg.littlewriter.domain.external.ai;

import com.pkg.littlewriter.domain.bookCreationHelper.imageGenerator.ImageGenerator;
import com.pkg.littlewriter.domain.bookCreationHelper.imageGenerator.ImageGeneratorInputDto;
import com.pkg.littlewriter.domain.bookCreationHelper.imageGenerator.ImageGeneratorResponseDto;
import com.pkg.littlewriter.domain.external.ai.exceptions.AiException;
import com.pkg.littlewriter.domain.external.ai.input.GenerateImageInputDto;
import com.pkg.littlewriter.domain.external.ai.modelBehavior.GenerateIllustrationBehavior;
import com.pkg.littlewriter.domain.external.ai.response.GenerateImageResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiImageGenerator extends Ai<GenerateImageInputDto, GenerateImageResponseDto> implements ImageGenerator {
    @Autowired
    public AiImageGenerator(GenerateIllustrationBehavior generateIllustrationBehavior) {
        super(generateIllustrationBehavior);
    }

    @Override
    public ImageGeneratorResponseDto getResponseFrom(ImageGeneratorInputDto imageGeneratorInputDto) throws AiException {
        GenerateImageInputDto inputDto = new GenerateImageInputDto(imageGeneratorInputDto.getContext());
        GenerateImageResponseDto responseDto = aiModelBehavior.getResponseFrom(inputDto);
        return new ImageGeneratorResponseDto(responseDto.getImageUrl());
    }
}
