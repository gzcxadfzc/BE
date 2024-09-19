package com.pkg.littlewriter.domain.bookCreationHelper.imageGenerator;

import com.pkg.littlewriter.domain.external.ai.exceptions.AiException;

public interface ImageGenerator {
    ImageGeneratorResponseDto getResponseFrom(ImageGeneratorInputDto imageGeneratorInputDto) throws AiException;
}
