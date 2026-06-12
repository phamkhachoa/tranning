package com.onemount.news.model;

import com.onemount.news.model.enumeration.NewsStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "news")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class News extends AbstractAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(unique = true)
    private String slug;

    @Column(length = 1000)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String author;

    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NewsStatus status;
}
