package com.pkg.core.mapper;

import com.pkg.littlewriter.domain.bookProgressHelper.model.BookInProgress;
import com.pkg.littlewriter.domain.bookProgressHelper.model.BookToProgress;
import com.pkg.littlewriter.domain.bookProgressHelper.model.NextContext;
import com.pkg.core.input.GenerateContextQuestionInputDto;
import com.pkg.core.input.GenerateImageInputDto;
import org.springframework.stereotype.Component;

@Component
public class BookToProgressMapper {
    public GenerateContextQuestionInputDto toGenerateContextQuestionInputDto(BookToProgress bookToProgress) {
        BookInProgress bookInProgress = bookToProgress.getBookInProgress();
        NextContext nextContext = bookToProgress.getNextContext();
        return GenerateContextQuestionInputDto.builder()
                .currentContext(nextContext.getContext())
                .previousContext(bookInProgress.getPreviousContext())
                .personality(bookInProgress.getCharacter().getPersonality())
                .mainCharacterName(bookInProgress.getCharacter().getName())
                .build();
    }

    public GenerateImageInputDto toGenerateImageInputDto(BookToProgress bookToProgress) {
        NextContext nextContext = bookToProgress.getNextContext();
        return new GenerateImageInputDto(nextContext.getContext());
    }
}
