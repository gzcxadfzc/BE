package com.pkg.littlewriter.domain.bookProgressHelper.model;

import lombok.Builder;
import lombok.Getter;
import org.joda.time.DateTime;

import java.util.List;

@Builder
@Getter
public class Book {
    private final String title;
    private final String author;
    private final Character character;
    private final DateTime createDate;
    private final List<BookPage> bookPages;
}
