package com.onemount.news.service;

import com.onemount.news.dto.NewsListResponse;
import com.onemount.news.dto.NewsRequest;
import com.onemount.news.dto.NewsResponse;
import com.onemount.news.dto.NewsSummaryResponse;
import com.onemount.news.exception.BadRequestException;
import com.onemount.news.exception.NotFoundException;
import com.onemount.news.exception.NotImplementedException;
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

import static org.springframework.util.StringUtils.hasText;

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
        // TODO trainee: map entities to NewsSummaryResponse via NewsMapper and wrap metadata in NewsListResponse
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdOn").descending());

        Page<News> newsPage = newsRepository.searchByTitle(keyword, pageable);

        List<NewsSummaryResponse> summaries =
                newsPage.getContent().stream().map(newsMapper::toSummaryResponse).toList();

        NewsListResponse response = new NewsListResponse();
        response.setNewsContent(summaries);
        response.setPageNo(newsPage.getNumber());
        response.setPageSize(newsPage.getSize());
        response.setTotalElements(newsPage.getTotalElements());
        response.setTotalPages(newsPage.getTotalPages());
        response.setLast(newsPage.isLast());

        return response;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = NEWS_CACHE, key = "#id")
    public NewsResponse getNewsById(Long id) {
        // TODO trainee: implement detail API by id, throw NotFoundException when missing.
        // TODO trainee: keep @Cacheable so repeated detail requests are served from Redis cache.
        // TODO trainee: map the entity to NewsResponse via NewsMapper.
        return newsRepository.findById(id)
                .map(newsMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("News item not found with id: " + id));
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
        News existingNews = newsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("News item not found with id: " + id));

        if (hasText(newsRequest.getSlug())
                && !newsRequest.getSlug().equals(existingNews.getSlug())) {
            boolean slugExists = newsRepository.existsBySlugAndIdNot(newsRequest.getSlug(), id);
            if (slugExists) {
                throw new BadRequestException("Slug '" + newsRequest.getSlug() + "' is already taken by another article.");
            }
        }

        newsMapper.updateEntity(newsRequest, existingNews);

        News updatedNews = newsRepository.save(existingNews);
        return newsMapper.toResponse(updatedNews);
    }

    @CacheEvict(cacheNames = NEWS_CACHE, key = "#id")
    public void deleteNews(Long id) {
        // TODO trainee: implement delete API by checking existence, deleting the row, and evicting cache.
        if (!newsRepository.existsById(id)) {
            throw new NotFoundException("Cannot delete. News item not found with id: " + id);
        }

        newsRepository.deleteById(id);
    }

    private String resolveSlug(NewsRequest newsRequest) {
        String slug = hasText(newsRequest.getSlug()) ? newsRequest.getSlug() : newsRequest.getTitle();
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
