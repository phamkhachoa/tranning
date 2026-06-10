package com.onemount.news.dto;

import java.util.List;

public record NewsListResponse(
        List<NewsSummaryResponse> newsContent,
        int pageNo,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean isLast) {
}
