package com.example.urlshortener.analytics.repository;

import com.example.urlshortener.analytics.entity.ClickEventEntity;
import com.example.urlshortener.url.entity.ShortUrlEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ClickEventRepository extends JpaRepository<ClickEventEntity, UUID> {
    List<ClickEventEntity> findByUrl(ShortUrlEntity url);

    long countByUrl(ShortUrlEntity url);

    @Query("select max(event.clickedAt) from ClickEventEntity event where event.url = :url")
    LocalDateTime findLatestClickAt(@Param("url") ShortUrlEntity url);

    @Query(value = """
            select cast(clicked_at as date) as day, count(id) as redirects
            from click_events
            where url_id = :urlId
            group by cast(clicked_at as date)
            order by cast(clicked_at as date)
            """, nativeQuery = true)
    List<DailyClicksProjection> countDailyByUrlId(@Param("urlId") UUID urlId);

    @Query("select count(event.id) from ClickEventEntity event")
    long countAllRedirects();

    @Query("""
            select event.url.shortCode as shortCode, count(event.id) as redirects
            from ClickEventEntity event
            group by event.url.shortCode
            order by count(event.id) desc
            """)
    List<TopLinkProjection> findTopLinks(Pageable pageable);

    interface DailyClicksProjection {
        LocalDate getDay();
        long getRedirects();
    }

    interface TopLinkProjection {
        String getShortCode();
        long getRedirects();
    }
}
