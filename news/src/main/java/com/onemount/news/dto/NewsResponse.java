package com.onemount.news.dto;

import com.onemount.news.model.News;
import com.onemount.news.model.enumeration.NewsStatus;
import java.time.ZonedDateTime;

public record NewsResponse(
        Long id,
        String title,
        String slug,
        String summary,
        String content,
        String author,
        String thumbnailUrl,
        NewsStatus status,
        ZonedDateTime createdOn,
        ZonedDateTime lastModifiedOn) {

    public static NewsResponse fromModel(News news) {
        return new NewsResponse(
                news.getId(),
                news.getTitle(),
                news.getSlug(),
                news.getSummary(),
                news.getContent(),
                news.getAuthor(),
                news.getThumbnailUrl(),
                news.getStatus(),
                news.getCreatedOn(),
                news.getLastModifiedOn());
    }
}
