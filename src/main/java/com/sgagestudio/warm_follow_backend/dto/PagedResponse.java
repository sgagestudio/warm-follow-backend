package com.sgagestudio.warm_follow_backend.dto;

import java.util.List;

public record PagedResponse<T>(
        List<T> items,
        String next_cursor
) {
}
