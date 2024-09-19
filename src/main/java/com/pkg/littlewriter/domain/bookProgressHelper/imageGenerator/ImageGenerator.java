package com.pkg.littlewriter.domain.bookProgressHelper.imageGenerator;

import com.pkg.littlewriter.domain.bookProgressHelper.exceptions.BookProgressException;
import com.pkg.littlewriter.domain.bookProgressHelper.model.BookProgressDetails;
import com.pkg.littlewriter.domain.bookProgressHelper.model.Illustration;

public interface ImageGenerator {
    Illustration generateFrom(BookProgressDetails bookProgressDetails) throws BookProgressException;
}
