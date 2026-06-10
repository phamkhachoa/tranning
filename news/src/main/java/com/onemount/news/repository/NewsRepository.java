package com.onemount.news.repository;

import com.onemount.news.model.News;
import org.hibernate.validator.constraints.ParameterScriptAssert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query("SELECT n FROM News n WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<News> searchByTitle(@Param("keyword") String keyword, Pageable pageable);
}
