package com.swissquote.caa.common;

import java.util.List;
import org.springframework.data.domain.Page;

/** Stable paging envelope used by the API (decoupled from Spring Data's Page JSON). */
public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageDto<T> of(Page<T> page) {
        return new PageDto<>(page.getContent(), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages());
    }

    public static <T> PageDto<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageDto<>(content, page, size, totalElements, totalPages);
    }
}
