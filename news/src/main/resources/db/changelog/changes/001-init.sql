-- liquibase formatted sql
-- changeset training:news-001-init

CREATE TABLE news (
    id                 BIGSERIAL    PRIMARY KEY,
    title              VARCHAR(255) NOT NULL,
    slug               VARCHAR(255),
    summary            VARCHAR(1000),
    content            TEXT,
    author             VARCHAR(255),
    thumbnail_url      VARCHAR(255),
    status             VARCHAR(20)  NOT NULL,
    created_on         TIMESTAMP(6),
    created_by         VARCHAR(255),
    last_modified_on   TIMESTAMP(6),
    last_modified_by   VARCHAR(255),
    CONSTRAINT uk_news_slug UNIQUE (slug)
);
