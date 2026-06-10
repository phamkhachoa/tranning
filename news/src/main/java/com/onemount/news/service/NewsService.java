package com.onemount.news.service;

import com.onemount.news.dto.NewsListResponse;
import com.onemount.news.dto.NewsRequest;
import com.onemount.news.dto.NewsResponse;
import com.onemount.news.exception.BadRequestException;
import com.onemount.news.exception.NotImplementedException;
import com.onemount.news.mapper.NewsMapper;
import com.onemount.news.model.News;
import com.onemount.news.repository.NewsRepository;
import com.onemount.news.utils.Constants;
import java.text.Normalizer;
import java.util.Locale;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class NewsService {

    private static final String NEWS_CACHE = "news";

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;

    public NewsService(NewsRepository newsRepository, NewsMapper newsMapper) {
        this.newsRepository = newsRepository;
        this.newsMapper = newsMapper;
    }

    @Transactional(readOnly = true)
    public NewsListResponse getNews(String keyword, int pageNo, int pageSize) {
        // TODO trainee: implement list API with pagination and sort by createdOn DESC.
        // TODO trainee: when keyword has text, support approximate title search, for example title LIKE %keyword%.
        // TODO trainee: map entities to NewsSummaryResponse via NewsMapper and wrap metadata in NewsListResponse.
        throw new NotImplementedException("TODO: implement list news API with keyword fuzzy search by title.");
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = NEWS_CACHE, key = "#id")
    public NewsResponse getNewsById(Long id) {
        // TODO trainee: implement detail API by id, throw NotFoundException when missing.
        // TODO trainee: keep @Cacheable so repeated detail requests are served from Redis cache.
        // TODO trainee: map the entity to NewsResponse via NewsMapper.
        throw new NotImplementedException("TODO: implement news detail API with Redis cache.");
    }

    public NewsResponse createNews(NewsRequest newsRequest) {
        String slug = resolveSlug(newsRequest);
        if (newsRepository.existsBySlug(slug)) {
            throw new BadRequestException(Constants.ErrorCode.SLUG_ALREADY_EXISTED, slug);
        }
        News news = newsMapper.toEntity(newsRequest);
        news.setSlug(slug);
        return newsMapper.toResponse(newsRepository.save(news));
    }

    @CachePut(cacheNames = NEWS_CACHE, key = "#id")
    public NewsResponse updateNews(Long id, NewsRequest newsRequest) {
        // TODO trainee: implement update API by loading the existing news and validating duplicate slug.
        // TODO trainee: update fields through NewsMapper.updateEntity, save, return NewsResponse.
        // TODO trainee: keep @CachePut so the Redis cache is refreshed after a successful update.
        throw new NotImplementedException("TODO: implement update news API and refresh Redis cache.");
    }

    @CacheEvict(cacheNames = NEWS_CACHE, key = "#id")
    public void deleteNews(Long id) {
        // TODO trainee: implement delete API by checking existence, deleting the row, and evicting cache.
        throw new NotImplementedException("TODO: implement delete news API and evict Redis cache.");
    }

    private String resolveSlug(NewsRequest newsRequest) {
        String slug = StringUtils.hasText(newsRequest.getSlug()) ? newsRequest.getSlug() : newsRequest.getTitle();
        String normalized = Normalizer.normalize(slug, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return normalized.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s]+", "-");
    }
}
