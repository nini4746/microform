package com.microform.common.pagination;

public record PageRequest(int page, int size) {
    public PageRequest {
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
    }

    public int offset() { return page * size; }

    public static PageRequest of(Integer page, Integer size) {
        return new PageRequest(page == null ? 0 : page, size == null ? 20 : size);
    }
}
