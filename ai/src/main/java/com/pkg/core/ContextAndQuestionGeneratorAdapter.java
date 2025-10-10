package com.pkg.core;

import com.pkg.littlewriter.domain.bookProgressHelper.contextAndQuestionGenerator.ContextAndQuestionGenerator;
import com.pkg.littlewriter.domain.bookProgressHelper.exceptions.ContextAndQuestionCreationFailedException;
import com.pkg.littlewriter.domain.bookProgressHelper.model.BookToProgress;
import com.pkg.littlewriter.domain.bookProgressHelper.model.ContextAndQuestion;
import com.pkg.core.exceptions.AiException;
import com.pkg.core.input.GenerateContextQuestionInputDto;
import com.pkg.core.mapper.BookToProgressMapper;
import com.pkg.core.response.GenerateContextQuestionResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ContextAndQuestionGeneratorAdapter implements ContextAndQuestionGenerator {
    private final AiContextQuestionGenerator contextQuestionGenerator;
    private final BookToProgressMapper mapper;

    @Autowired
    public ContextAndQuestionGeneratorAdapter(AiContextQuestionGenerator contextQuestionGenerator, BookToProgressMapper mapper) {
        this.contextQuestionGenerator = contextQuestionGenerator;
        this.mapper = mapper;
    }

    @Override
    public ContextAndQuestion generateFrom(BookToProgress bookToProgress) throws ContextAndQuestionCreationFailedException {
        GenerateContextQuestionInputDto inputDto = mapper.toGenerateContextQuestionInputDto(bookToProgress);
        try {
            GenerateContextQuestionResponseDto responseDto = contextQuestionGenerator.getResponseFrom(inputDto);
            return new ContextAndQuestion(responseDto.getRefinedText(), responseDto.getQuestions());
        } catch (AiException e) {
            throw new ContextAndQuestionCreationFailedException(e.getMessage());
        }
    }
}
