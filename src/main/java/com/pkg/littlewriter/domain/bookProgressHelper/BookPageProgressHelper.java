package com.pkg.littlewriter.domain.bookProgressHelper;

import com.pkg.littlewriter.domain.bookProgressHelper.exceptions.BookProgressException;
import com.pkg.littlewriter.domain.bookProgressHelper.model.BookPage;
import com.pkg.littlewriter.domain.bookProgressHelper.model.BookToProgress;

public interface BookPageProgressHelper {
    BookPage continueStory(BookToProgress bookProgressDetails) throws BookProgressException;
}
