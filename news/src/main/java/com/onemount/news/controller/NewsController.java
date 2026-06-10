package com.onemount.news.controller;

import com.onemount.news.dto.BaseResponse;
import com.onemount.news.dto.NewsListResponse;
import com.onemount.news.dto.NewsRequest;
import com.onemount.news.dto.NewsResponse;
import com.onemount.news.service.NewsService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public ResponseEntity<BaseResponse<NewsListResponse>> getNews(
            @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(BaseResponse.success(newsService.getNews(pageNo, pageSize)));
    }

    @GetMapping("/{id}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class)))})
    public ResponseEntity<BaseResponse<NewsResponse>> getNewsById(@PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.success(newsService.getNewsById(id)));
    }

    @PostMapping
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class)))})
    public ResponseEntity<BaseResponse<NewsResponse>> createNews(@Valid @RequestBody NewsRequest newsRequest) {
        NewsResponse newsResponse = newsService.createNews(newsRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(HttpStatus.CREATED.value(), newsResponse));
    }

    @PutMapping("/{id}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class)))})
    public ResponseEntity<BaseResponse<NewsResponse>> updateNews(
            @PathVariable Long id, @Valid @RequestBody NewsRequest newsRequest) {
        return ResponseEntity.ok(BaseResponse.success(newsService.updateNews(id, newsRequest)));
    }

    @DeleteMapping("/{id}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class)))})
    public ResponseEntity<BaseResponse<Void>> deleteNews(@PathVariable Long id) {
        newsService.deleteNews(id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
