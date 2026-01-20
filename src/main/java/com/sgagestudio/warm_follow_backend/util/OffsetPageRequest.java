package com.sgagestudio.warm_follow_backend.util;

import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class OffsetPageRequest extends AbstractPageRequest {
    private final long offset;
    private final Sort sort;

    public OffsetPageRequest(long offset, int pageSize, Sort sort) {
        super((int) (offset / pageSize), pageSize);
        this.offset = offset;
        this.sort = sort == null ? Sort.unsorted() : sort;
    }

    public static OffsetPageRequest of(long offset, int pageSize, Sort sort) {
        return new OffsetPageRequest(offset, pageSize, sort);
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageRequest(offset + getPageSize(), getPageSize(), sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageRequest((long) pageNumber * getPageSize(), getPageSize(), sort);
    }

    @Override
    public Pageable previous() {
        long newOffset = Math.max(offset - getPageSize(), 0);
        return new OffsetPageRequest(newOffset, getPageSize(), sort);
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(0, getPageSize(), sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
