package com.onemount.news.mapper;

import com.onemount.news.dto.NewsRequest;
import com.onemount.news.dto.NewsResponse;
import com.onemount.news.dto.NewsSummaryResponse;
import com.onemount.news.model.News;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NewsMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    News toEntity(NewsRequest newsRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    void updateEntity(NewsRequest newsRequest, @MappingTarget News news);

    NewsResponse toResponse(News news);

    NewsSummaryResponse toSummaryResponse(News news);

    List<NewsSummaryResponse> toSummaryResponses(List<News> news);
}
