package com.pkg.domain.common;

import java.util.List;

public class PageResult<T> {

    private final List<T> elements;
    private final PageInfo pageInfo;

    public PageResult(List<T> elements, PageInfo pageInfo) {
        this.elements = elements;
        this.pageInfo = pageInfo;
    }

    public List<T> getElements() {
        return elements;
    }

    public PageInfo getPageInfo() {
        return pageInfo;
    }
}
