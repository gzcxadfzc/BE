package com.pkg.littlewriter.domain.bookProgressHelper.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Illustration {
    private final String url;

    public Illustration(String url) {
        this.url = url;
    }
}
