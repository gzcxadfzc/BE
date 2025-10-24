package com.pkg.jpa;

import com.pkg.domain.book.BookRetrieveQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class BookRetrieveQueryMapper {

    public static Pageable toPageable(BookRetrieveQuery query) {
        if (query == null) {
            return PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        Sort sort = switch (query.sort()) {
            case CREATED_AT_DESC -> Sort.by(Sort.Direction.DESC, "createdAt");
            case CREATED_AT_ASC -> Sort.by(Sort.Direction.ASC, "createdAt");
            case TITLE_DESC -> Sort.by(Sort.Direction.DESC, "title");
            case TITLE_ASC -> Sort.by(Sort.Direction.ASC, "title");
        };

        return PageRequest.of(query.index(), query.size(), sort);
    }
}
