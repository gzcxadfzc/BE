package com.pkg.domain.common;

import java.util.List;

public class SliceResult<T> {

    private final List<T> elements;
    private final int index;
    private final boolean hasNext;

    public SliceResult(List<T> elements, int index, boolean hasNext) {
        this.elements = elements;
        this.index = index;
        this.hasNext = hasNext;
    }

    public List<T> getElements() { return elements; }
    public int getIndex()        { return index; }
    public boolean isHasNext()   { return hasNext; }
}
