package com.onemount.news.dto;

import com.onemount.news.model.News;
import com.onemount.news.model.enumeration.NewsStatus;
import java.time.ZonedDateTime;

public record NewsSummaryResponse(
        Long id,
        String title,
        String slug,
        String summary,
        String author,
        String thumbnailUrl,
        NewsStatus status,
        ZonedDateTime createdOn) {

    public static NewsSummaryResponse fromModel(News news) {
        return new NewsSummaryResponse(
                news.getId(),
                news.getTitle(),
                news.getSlug(),
                news.getSummary(),
                news.getAuthor(),
                news.getThumbnailUrl(),
                news.getStatus(),
                news.getCreatedOn());
    }
}
