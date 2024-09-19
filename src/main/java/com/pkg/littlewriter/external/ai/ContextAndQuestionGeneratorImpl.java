package com.pkg.littlewriter.external.ai;

import com.pkg.littlewriter.domain.bookProgressHelper.contextAndQuestionGenerator.ContextAndQuestionGenerator;
import com.pkg.littlewriter.domain.bookProgressHelper.exceptions.BookProgressException;
import com.pkg.littlewriter.domain.bookProgressHelper.model.BookInProgress;
import com.pkg.littlewriter.domain.bookProgressHelper.model.BookProgressDetails;
import com.pkg.littlewriter.domain.bookProgressHelper.model.ContextAndQuestion;
import com.pkg.littlewriter.external.ai.exceptions.AiException;
import com.pkg.littlewriter.external.ai.input.GenerateContextQuestionInputDto;
import com.pkg.littlewriter.external.ai.response.GenerateContextQuestionResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ContextAndQuestionGeneratorImpl implements ContextAndQuestionGenerator {
    private final ContextQuestionGenerator contextQuestionGenerator;

    @Autowired
    public ContextAndQuestionGeneratorImpl(ContextQuestionGenerator contextQuestionGenerator) {
        this.contextQuestionGenerator = contextQuestionGenerator;
    }

    @Override
    public ContextAndQuestion generateFrom(BookProgressDetails bookProgressDetails) throws BookProgressException {
        GenerateContextQuestionInputDto inputDto = getGenerateContextQuestionInputDto(bookProgressDetails);
        try {
            GenerateContextQuestionResponseDto responseDto = contextQuestionGenerator.getResponseFrom(inputDto);
            return new ContextAndQuestion(responseDto.getRefinedText(), responseDto.getQuestions());
        } catch (AiException e) {
            throw new BookProgressException(e.getMessage());
        }
    }

    private static GenerateContextQuestionInputDto getGenerateContextQuestionInputDto(BookProgressDetails bookProgressDetails) {
        BookInProgress bookInProgress = bookProgressDetails.getBookInProgress();
        return GenerateContextQuestionInputDto.builder()
                .personality(bookInProgress.getCharacter().getPersonality())
                .previousContext(bookInProgress.getPreviousContext())
                .currentContext(bookProgressDetails.getNextContext().getContext())
                .mainCharacterName(bookInProgress.getCharacter().getName())
                .build();
    }
}
