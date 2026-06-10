package com.onemount.news.dto;

import com.onemount.news.model.enumeration.NewsStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewsRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 255)
    private String slug;

    @Size(max = 1000)
    private String summary;

    private String content;

    @Size(max = 255)
    private String author;

    @Size(max = 255)
    private String thumbnailUrl;

    @NotNull
    private NewsStatus status;
}
