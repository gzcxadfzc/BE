package com.pkg.littlewriter.external.ai;

import com.pkg.littlewriter.domain.bookProgressHelper.exceptions.BookProgressException;
import com.pkg.littlewriter.domain.bookProgressHelper.imageGenerator.ImageGenerator;
import com.pkg.littlewriter.domain.bookProgressHelper.model.BookToProgress;
import com.pkg.littlewriter.domain.bookProgressHelper.model.Illustration;
import com.pkg.littlewriter.external.ai.exceptions.AiException;
import com.pkg.littlewriter.external.ai.input.GenerateImageInputDto;
import com.pkg.littlewriter.external.ai.mapper.BookToProgressMapper;
import com.pkg.littlewriter.external.ai.response.GenerateImageResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ImageGeneratorAdapter implements ImageGenerator {
    private final AiImageGenerator aiImageGenerator;
    private final BookToProgressMapper mapper;

    @Autowired
    public ImageGeneratorAdapter(AiImageGenerator aiImageGenerator, BookToProgressMapper mapper) {
        this.aiImageGenerator = aiImageGenerator;
        this.mapper = mapper;
    }

    @Override
    public Illustration generateFrom(BookToProgress bookToProgress) throws BookProgressException {
        GenerateImageInputDto generateImageInputDto = mapper.toGenerateImageInputDto(bookToProgress);
        try {
            GenerateImageResponseDto responseDto = aiImageGenerator.getResponseFrom(generateImageInputDto);
            return new Illustration(responseDto.getImageUrl());
        } catch (AiException e) {
            throw new BookProgressException(e.getMessage());
        }
    }
}
