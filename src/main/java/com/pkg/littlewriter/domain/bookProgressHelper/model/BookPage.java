package com.pkg.littlewriter.domain.bookProgressHelper.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BookPage {
    private final ContextAndQuestion contextAndQuestion;
    private final Illustration illustration;
}
