package com.pkg.controller.book;

import com.pkg.domain.book.BookRetrieveQuery;
import org.springframework.util.MultiValueMap;

public class BookQueryParamMapper {

    private static final BookRetrieveQuery.SortOption DEFAULT_SORT_OPTION = BookRetrieveQuery.SortOption.CREATED_AT_ASC;
    private static final int DEFAULT_INDEX = 0;
    private static final int DEFAULT_SIZE = 10;

    public static BookRetrieveQuery toDomainQuery(MultiValueMap<String, String> param) {

        BookRetrieveQuery.SortOption sortOption = DEFAULT_SORT_OPTION;
        int index = DEFAULT_INDEX;
        int size = DEFAULT_SIZE;

        if(param.containsKey("sort")) {
            String sortValue = param.getFirst("sort");
            if(sortValue.equals("createdAtDesc")) {
                sortOption = BookRetrieveQuery.SortOption.CREATED_AT_DESC;
            }
            if(sortValue.equals("createdAtAsc")) {
                sortOption = BookRetrieveQuery.SortOption.CREATED_AT_ASC;
            }
            if(sortValue.equals("titleDesc")) {
                sortOption = BookRetrieveQuery.SortOption.TITLE_DESC;
            }
            if(sortValue.equals("titleAsc")) {
                sortOption = BookRetrieveQuery.SortOption.TITLE_ASC;
            }
        }
        if(param.containsKey("index")) {
            index = Integer.parseInt(param.getFirst("index"));
        }
        if(param.containsKey("size")) {
            size = Integer.parseInt(param.getFirst("size"));
        }
        return new BookRetrieveQuery(index, size, sortOption);
    }
}
