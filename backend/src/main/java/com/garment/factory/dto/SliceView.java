package com.garment.factory.dto;

import java.util.List;

import org.springframework.data.domain.Slice;

/**
 * 滚动分页返回体：只有 hasNext，没有总页数，配合前端「加载更多」。
 */
public class SliceView<T> {

    private final List<T> content;
    private final boolean hasNext;
    private final int page;
    private final int size;

    private SliceView(List<T> content, boolean hasNext, int page, int size) {
        this.content = content;
        this.hasNext = hasNext;
        this.page = page;
        this.size = size;
    }

    public static <T> SliceView<T> of(Slice<T> slice) {
        return new SliceView<>(slice.getContent(), slice.hasNext(),
                slice.getNumber(), slice.getSize());
    }

    public List<T> getContent() {
        return content;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}
