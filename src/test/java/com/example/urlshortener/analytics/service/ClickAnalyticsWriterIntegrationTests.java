package com.example.urlshortener.analytics.service;

import com.example.urlshortener.analytics.repository.ClickEventRepository;
import com.example.urlshortener.auth.repository.RefreshTokenRepository;
import com.example.urlshortener.url.entity.ShortUrlEntity;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.entity.UserRole;
import com.example.urlshortener.user.entity.UserStatus;
import com.example.urlshortener.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ClickAnalyticsWriterIntegrationTests {
    @Autowired ClickAnalyticsWriter writer;
    @Autowired UserRepository userRepository;
    @Autowired ShortUrlRepository shortUrlRepository;
    @Autowired ClickEventRepository clickEventRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        clickEventRepository.deleteAll();
        shortUrlRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void batchPersistenceShouldBeIdempotentAndUseAtomicCounterUpdates() {
        UserEntity owner = userRepository.saveAndFlush(new UserEntity(UUID.randomUUID(), "owner@example.com", "hash", UserRole.USER, UserStatus.ACTIVE));
        ShortUrlEntity url = shortUrlRepository.saveAndFlush(new ShortUrlEntity(UUID.randomUUID(), "abc1234", "https://example.com", null, owner, LocalDateTime.now().plusDays(1)));
        UUID eventId = UUID.randomUUID();
        ClickAnalyticsEvent event = new ClickAnalyticsEvent(eventId, url.getId(), LocalDateTime.now(), "hash", "desktop", "example.org", "corr-1");

        assertThat(writer.persistBatch(List.of(event, event))).isEqualTo(1);

        ShortUrlEntity updated = shortUrlRepository.findById(url.getId()).orElseThrow();
        assertThat(updated.getClickCount()).isEqualTo(1);
        assertThat(clickEventRepository.count()).isEqualTo(1);
        assertThat(clickEventRepository.findAll().getFirst().getReferer()).isEqualTo("example.org");
        assertThat(clickEventRepository.findAll().getFirst().getUserAgent()).isEqualTo("desktop");
    }
}
