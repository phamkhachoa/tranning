package com.onemount.news.dto;

import com.onemount.news.model.enumeration.NewsStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NewsRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 255) String slug,
        @Size(max = 1000) String summary,
        String content,
        @Size(max = 255) String author,
        @Size(max = 255) String thumbnailUrl,
        @NotNull NewsStatus status) {
}
