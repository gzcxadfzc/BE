package com.pkg.littlewriter.domain.bookProgressHelper.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookToProgress {
    private final NextContext nextContext;
    private final BookInProgress bookInProgress;
}
