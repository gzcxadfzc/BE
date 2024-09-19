package com.pkg.littlewriter.domain.bookProgressHelper.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BookInProgress {
    private final Character character;
    private final String previousContext;
}
