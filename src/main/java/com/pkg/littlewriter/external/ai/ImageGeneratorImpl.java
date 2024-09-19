package com.pkg.littlewriter.external.ai;

import com.pkg.littlewriter.domain.bookProgressHelper.exceptions.BookProgressException;
import com.pkg.littlewriter.domain.bookProgressHelper.imageGenerator.ImageGenerator;
import com.pkg.littlewriter.domain.bookProgressHelper.model.BookProgressDetails;
import com.pkg.littlewriter.domain.bookProgressHelper.model.Illustration;
import com.pkg.littlewriter.domain.bookProgressHelper.model.NextContext;
import com.pkg.littlewriter.external.ai.exceptions.AiException;
import com.pkg.littlewriter.external.ai.input.GenerateImageInputDto;
import com.pkg.littlewriter.external.ai.response.GenerateImageResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ImageGeneratorImpl implements ImageGenerator {
    private final AiImageGenerator aiImageGenerator;

    @Autowired
    public ImageGeneratorImpl(AiImageGenerator aiImageGenerator) {
        this.aiImageGenerator = aiImageGenerator;
    }
    @Override
    public Illustration generateFrom(BookProgressDetails bookProgressDetails) throws BookProgressException {
        NextContext nextContext = bookProgressDetails.getNextContext();
        try {
            GenerateImageInputDto generateImageInputDto = new GenerateImageInputDto(nextContext.getContext());
            GenerateImageResponseDto responseDto = aiImageGenerator.getResponseFrom(generateImageInputDto);
            return new Illustration(responseDto.getImageUrl());
        } catch (AiException e) {
            throw new BookProgressException(e.getMessage());
        }
    }
}
