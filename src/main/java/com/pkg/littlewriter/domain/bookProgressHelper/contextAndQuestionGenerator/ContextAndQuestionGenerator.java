package com.pkg.littlewriter.domain.bookProgressHelper.contextAndQuestionGenerator;

import com.pkg.littlewriter.domain.bookProgressHelper.exceptions.BookProgressException;
import com.pkg.littlewriter.domain.bookProgressHelper.model.BookToProgress;
import com.pkg.littlewriter.domain.bookProgressHelper.model.ContextAndQuestion;

public interface ContextAndQuestionGenerator {
    ContextAndQuestion generateFrom(BookToProgress BookToProgress) throws BookProgressException;
}
