package com.pkg.littlewriter.domain.bookProgressHelper.contextAndQuestionGenerator;

import com.pkg.littlewriter.domain.bookProgressHelper.exceptions.BookProgressException;
import com.pkg.littlewriter.domain.bookProgressHelper.model.BookProgressDetails;
import com.pkg.littlewriter.domain.bookProgressHelper.model.ContextAndQuestion;

public interface ContextAndQuestionGenerator {
    ContextAndQuestion generateFrom(BookProgressDetails bookProgressDetails) throws BookProgressException;
}
