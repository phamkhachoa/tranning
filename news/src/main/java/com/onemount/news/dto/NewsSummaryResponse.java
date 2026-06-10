package com.onemount.news.dto;

import com.onemount.news.model.enumeration.NewsStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewsSummaryResponse {

    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String author;
    private String thumbnailUrl;
    private NewsStatus status;
    private Instant createdOn;
}
