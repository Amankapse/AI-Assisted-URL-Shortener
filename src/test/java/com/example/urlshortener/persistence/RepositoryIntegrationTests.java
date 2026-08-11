package com.example.urlshortener.persistence;

import com.example.urlshortener.analytics.entity.ClickEventEntity;
import com.example.urlshortener.analytics.repository.ClickEventRepository;
import com.example.urlshortener.apikey.repository.ApiKeyRepository;
import com.example.urlshortener.auth.repository.RefreshTokenRepository;
import com.example.urlshortener.url.entity.ShortUrlEntity;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.entity.UserRole;
import com.example.urlshortener.user.entity.UserStatus;
import com.example.urlshortener.user.repository.UserRepository;
import com.example.urlshortener.workspace.entity.WorkspaceEntity;
import com.example.urlshortener.workspace.repository.WorkspaceMembershipRepository;
import com.example.urlshortener.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.example.urlshortener.testsupport.WorkspaceTestSupport.defaultWorkspace;

@SpringBootTest
@ActiveProfiles("test")
class RepositoryIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        apiKeyRepository.deleteAll();
        clickEventRepository.deleteAll();
        shortUrlRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void flywayShouldCreateExpectedTablesAndIndexes() {
        List<String> tables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'",
                String.class);
        List<String> indexes = jdbcTemplate.queryForList(
                "select indexname from pg_indexes where schemaname = 'public'",
                String.class);

        assertThat(tables).contains("users", "short_urls", "click_events", "refresh_tokens", "workspaces", "workspace_memberships", "audit_events", "api_keys", "flyway_schema_history");
        assertThat(indexes).contains(
                "idx_short_urls_short_code",
                "idx_short_urls_owner_id",
                "idx_short_urls_enabled_expires_at",
                "idx_short_urls_blocked",
                "idx_click_events_url_id",
                "idx_click_events_clicked_at",
                "idx_refresh_tokens_user_id",
                "idx_refresh_tokens_family_id",
                "idx_refresh_tokens_expires_at",
                "idx_refresh_tokens_active_lookup",
                "idx_click_events_url_clicked_at",
                "idx_workspace_memberships_user_id",
                "idx_short_urls_workspace_created_at",
                "idx_short_urls_workspace_short_code",
                "idx_audit_events_workspace_occurred_at",
                "idx_audit_events_resource_occurred_at",
                "idx_audit_events_actor_occurred_at",
                "idx_audit_events_action_occurred_at",
                "uq_api_keys_key_prefix",
                "uq_api_keys_key_digest",
                "idx_api_keys_workspace_revoked_at",
                "idx_api_keys_workspace_created_at"
        );
        String metadataType = jdbcTemplate.queryForObject(
                "select data_type from information_schema.columns where table_name = 'audit_events' and column_name = 'metadata'",
                String.class);
        assertThat(metadataType).isEqualTo("jsonb");
    }

    @Test
    void shouldEnforceUniqueEmailAndShortCodeConstraints() {
        UserEntity owner = user("owner@example.com");
        userRepository.saveAndFlush(owner);

        assertThatThrownBy(() -> userRepository.saveAndFlush(user("owner@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);

        shortUrlRepository.saveAndFlush(shortUrl("ABC1234", "https://example.com/a", owner));
        assertThatThrownBy(() -> shortUrlRepository.saveAndFlush(shortUrl("ABC1234", "https://example.com/b", owner)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldEnforceForeignKeyIntegrityForClickEvents() {
        UserEntity owner = userRepository.saveAndFlush(user("owner@example.com"));
        ShortUrlEntity url = shortUrlRepository.saveAndFlush(shortUrl("ABC1234", "https://example.com/a", owner));

        clickEventRepository.saveAndFlush(new ClickEventEntity(UUID.randomUUID(), url, LocalDateTime.now(), "hash", "ua", "ref"));

        assertThat(clickEventRepository.findByUrl(url)).hasSize(1);
    }

    @Test
    void shouldFilterAndPageByOwnerAndResolveShortCode() {
        UserEntity owner = userRepository.saveAndFlush(user("owner@example.com"));
        UserEntity other = userRepository.saveAndFlush(user("other@example.com"));
        ShortUrlEntity first = shortUrlRepository.saveAndFlush(shortUrl("ABC1234", "https://example.com/a", owner));
        ShortUrlEntity legacy = shortUrlRepository.saveAndFlush(shortUrl("LEGACY7", "https://example.com/legacy", owner));
        shortUrlRepository.saveAndFlush(shortUrl("DEF5678", "https://example.com/b", owner));
        shortUrlRepository.saveAndFlush(shortUrl("GHI9012", "https://example.com/c", other));

        assertThat(shortUrlRepository.findByOwner(owner, PageRequest.of(0, 1)).getContent()).hasSize(1);
        assertThat(shortUrlRepository.findByOwner(owner, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(3);
        assertThat(shortUrlRepository.findByOwner(other, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
        assertThat(shortUrlRepository.findByShortCode("ABC1234"))
                .map(ShortUrlEntity::getId)
                .contains(first.getId());
        assertThat(shortUrlRepository.findByShortCode("LEGACY7"))
                .map(ShortUrlEntity::getId)
                .contains(legacy.getId());
        assertThat(shortUrlRepository.findByIdAndOwner(first.getId(), other)).isEmpty();
        assertThat(shortUrlRepository.findByIdAndWorkspaceId(first.getId(), first.getWorkspace().getId())).isPresent();
    }

    @Test
    void shouldPersistBlockedModerationState() {
        UserEntity owner = userRepository.saveAndFlush(user("owner@example.com"));
        ShortUrlEntity entity = shortUrlRepository.saveAndFlush(shortUrl("BLOCKED1", "https://example.com/a", owner));

        entity.setBlocked(true);
        shortUrlRepository.saveAndFlush(entity);

        assertThat(shortUrlRepository.findById(entity.getId())).map(ShortUrlEntity::isBlocked).contains(true);
        assertThat(shortUrlRepository.countActiveByOwner(owner)).isZero();
    }

    @Test
    void shouldIncrementOptimisticVersionOnUpdate() {
        UserEntity owner = userRepository.saveAndFlush(user("owner@example.com"));
        ShortUrlEntity entity = shortUrlRepository.saveAndFlush(shortUrl("ABC1234", "https://example.com/a", owner));
        long initialVersion = entity.getVersion();

        entity.setEnabled(false);
        ShortUrlEntity updated = shortUrlRepository.saveAndFlush(entity);

        assertThat(updated.getVersion()).isGreaterThan(initialVersion);
    }

    private UserEntity user(String email) {
        return new UserEntity(UUID.randomUUID(), email, "hash", UserRole.USER, UserStatus.ACTIVE);
    }

    private ShortUrlEntity shortUrl(String shortCode, String originalUrl, UserEntity owner) {
        WorkspaceEntity workspace = defaultWorkspace(owner, workspaceRepository, workspaceMembershipRepository);
        return new ShortUrlEntity(UUID.randomUUID(), shortCode, originalUrl, null, owner, workspace, LocalDateTime.now().plusDays(1));
    }
}
