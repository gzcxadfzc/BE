package com.pkg.domain.book;

public record BookRetrieveQuery(
        int index,
        int size,
        SortOption sort
) {

    public enum SortOption {

        CREATED_AT_DESC,
        CREATED_AT_ASC,
        TITLE_DESC,
        TITLE_ASC,
        ;
    }
}
