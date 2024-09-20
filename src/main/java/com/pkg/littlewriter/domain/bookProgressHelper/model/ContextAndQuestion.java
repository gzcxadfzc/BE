package com.pkg.littlewriter.domain.bookProgressHelper.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ContextAndQuestion {
    private final String refinedText;
    private final List<String> questions;
}
