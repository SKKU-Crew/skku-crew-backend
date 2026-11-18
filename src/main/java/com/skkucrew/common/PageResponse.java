package com.skkucrew.common;

import java.util.List;

/** 목록 응답 (공고 목록은 20개 단위, FR-SRCH-05) */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {
    public static <T> PageResponse<T> of(List<T> all, int page, int size) {
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        int safePage = Math.max(page, 0);
        int from = Math.min(safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new PageResponse<>(List.copyOf(all.subList(from, to)), safePage, safeSize, all.size(), to < all.size());
    }
}
