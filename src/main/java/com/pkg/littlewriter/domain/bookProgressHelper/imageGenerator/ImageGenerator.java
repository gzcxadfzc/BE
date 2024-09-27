package com.pkg.littlewriter.domain.bookProgressHelper.imageGenerator;

import com.pkg.littlewriter.domain.bookProgressHelper.exceptions.BookProgressException;
import com.pkg.littlewriter.domain.bookProgressHelper.model.BookToProgress;
import com.pkg.littlewriter.domain.bookProgressHelper.model.Illustration;

public interface ImageGenerator {
    Illustration generateFrom(BookToProgress bookProgressDetails) throws BookProgressException;
}
