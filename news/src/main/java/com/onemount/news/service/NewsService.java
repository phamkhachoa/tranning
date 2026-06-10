package com.onemount.news.service;

import com.onemount.news.dto.NewsListResponse;
import com.onemount.news.dto.NewsRequest;
import com.onemount.news.dto.NewsResponse;
import com.onemount.news.dto.NewsSummaryResponse;
import com.onemount.news.exception.BadRequestException;
import com.onemount.news.exception.NotFoundException;
import com.onemount.news.mapper.NewsMapper;
import com.onemount.news.model.News;
import com.onemount.news.repository.NewsRepository;
import com.onemount.news.utils.Constants;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public NewsListResponse getNews(int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdOn"));
        Page<News> newsPage = newsRepository.findAll(pageable);
        List<NewsSummaryResponse> newsSummaries = newsMapper.toSummaryResponses(newsPage.getContent());
        return new NewsListResponse(
                newsSummaries,
                newsPage.getNumber(),
                newsPage.getSize(),
                newsPage.getTotalElements(),
                newsPage.getTotalPages(),
                newsPage.isLast());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = NEWS_CACHE, key = "#id")
    public NewsResponse getNewsById(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(Constants.ErrorCode.NEWS_NOT_FOUND, id));
        return newsMapper.toResponse(news);
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
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(Constants.ErrorCode.NEWS_NOT_FOUND, id));
        String slug = resolveSlug(newsRequest);
        if (newsRepository.existsBySlugAndIdNot(slug, id)) {
            throw new BadRequestException(Constants.ErrorCode.SLUG_ALREADY_EXISTED, slug);
        }
        newsMapper.updateEntity(newsRequest, news);
        news.setSlug(slug);
        return newsMapper.toResponse(newsRepository.save(news));
    }

    @CacheEvict(cacheNames = NEWS_CACHE, key = "#id")
    public void deleteNews(Long id) {
        if (!newsRepository.existsById(id)) {
            throw new NotFoundException(Constants.ErrorCode.NEWS_NOT_FOUND, id);
        }
        newsRepository.deleteById(id);
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
