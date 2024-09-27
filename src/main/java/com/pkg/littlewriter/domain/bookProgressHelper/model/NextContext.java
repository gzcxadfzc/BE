package com.pkg.littlewriter.domain.bookProgressHelper.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NextContext {
    private final String context;

    public NextContext(String context) {
        this.context = context;
    }
}
