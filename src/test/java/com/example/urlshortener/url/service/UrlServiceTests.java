package com.example.urlshortener.url.service;

import com.example.urlshortener.audit.service.AuditService;
import com.example.urlshortener.audit.entity.AuditActorType;
import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.common.exception.PreconditionFailedException;
import com.example.urlshortener.common.exception.ResourceNotFoundException;
import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.common.ratelimit.RateLimiterService;
import com.example.urlshortener.outbox.domain.DomainEventPublisher;
import com.example.urlshortener.url.config.ShortCodeProperties;
import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.dto.ShortUrlResponse;
import com.example.urlshortener.url.dto.UpdateDestinationRequest;
import com.example.urlshortener.url.dto.UpdateShortUrlRequest;
import com.example.urlshortener.url.entity.ShortUrlEntity;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.entity.UserRole;
import com.example.urlshortener.user.entity.UserStatus;
import com.example.urlshortener.user.repository.UserRepository;
import com.example.urlshortener.user.service.CurrentOwnerProvider;
import com.example.urlshortener.user.service.OwnerIdentity;
import com.example.urlshortener.workspace.entity.WorkspaceEntity;
import com.example.urlshortener.workspace.entity.WorkspaceRole;
import com.example.urlshortener.workspace.service.WorkspaceContext;
import com.example.urlshortener.workspace.service.WorkspaceContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UrlServiceTests {

    private static final String PLACEHOLDER_EMAIL = "placeholder@example.com";

    private ShortUrlRepository shortUrlRepository;
    private UrlValidationService validationService;
    private ShortCodeGenerator shortCodeGenerator;
    private CurrentOwnerProvider ownerProvider;
    private UserRepository userRepository;
    private ApplicationEventPublisher eventPublisher;
    private RateLimiterService rateLimiter;
    private AppMetrics metrics;
    private ShortCodeProperties shortCodeProperties;
    private UrlQuotaService quotaService;
    private PublicUrlBuilder publicUrlBuilder;
    private WorkspaceContextResolver workspaceContextResolver;
    private AuditService auditService;
    private DomainEventPublisher domainEventPublisher;
    private WorkspaceEntity workspace;
    private UUID workspaceId;
    private UrlService urlService;

    @BeforeEach
    void setUp() {
        shortUrlRepository = Mockito.mock(ShortUrlRepository.class);
        validationService = Mockito.mock(UrlValidationService.class);
        shortCodeGenerator = Mockito.mock(ShortCodeGenerator.class);
        ownerProvider = Mockito.mock(CurrentOwnerProvider.class);
        userRepository = Mockito.mock(UserRepository.class);
        eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        rateLimiter = Mockito.mock(RateLimiterService.class);
        metrics = Mockito.mock(AppMetrics.class);
        shortCodeProperties = new ShortCodeProperties();
        quotaService = Mockito.mock(UrlQuotaService.class);
        publicUrlBuilder = Mockito.mock(PublicUrlBuilder.class);
        workspaceContextResolver = Mockito.mock(WorkspaceContextResolver.class);
        auditService = Mockito.mock(AuditService.class);
        domainEventPublisher = Mockito.mock(DomainEventPublisher.class);
        workspaceId = UUID.randomUUID();
        workspace = new WorkspaceEntity(workspaceId, "Test Workspace", true, null);
        when(workspaceContextResolver.resolveForLinkWriter(any())).thenReturn(new WorkspaceContext(workspaceId, UUID.randomUUID(), AuditActorType.USER, WorkspaceRole.OWNER, workspace));
        when(workspaceContextResolver.resolveForLinkReader(any())).thenReturn(new WorkspaceContext(workspaceId, UUID.randomUUID(), AuditActorType.USER, WorkspaceRole.OWNER, workspace));
        when(publicUrlBuilder.shortUrl(any())).thenAnswer(invocation -> "http://localhost:8080/r/" + invocation.getArgument(0));
        urlService = new UrlService(shortUrlRepository, validationService, shortCodeGenerator, ownerProvider, userRepository, eventPublisher, rateLimiter, metrics, shortCodeProperties, quotaService, publicUrlBuilder, workspaceContextResolver, auditService, domainEventPublisher);
    }

    @Test
    void createShouldGenerateShortCodeWhenAliasNotProvided() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://example.com/page");
        request.setExpiresAt(LocalDateTime.now().plusDays(1));

        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(shortCodeGenerator.generate()).thenReturn("ABC1234");
        when(shortUrlRepository.existsByShortCode("ABC1234")).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrlEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrlResponse response = urlService.create(request);
        assertThat(response.getShortCode()).isEqualTo("ABC1234");
        assertThat(response.getShortUrl()).isEqualTo("http://localhost:8080/r/ABC1234");
        verify(quotaService).enforceCreateQuota(workspaceId, request);
        verify(metrics).shortCodeGeneration("success");
    }

    @Test
    void createShouldRejectDuplicateCustomAlias() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://example.com/page");
        request.setCustomAlias("alias123");
        request.setExpiresAt(LocalDateTime.now().plusDays(1));

        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(shortUrlRepository.existsByCustomAlias("alias123")).thenReturn(true);

        assertThatThrownBy(() -> urlService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("customAlias is already in use");
    }

    @Test
    void createShouldUseCustomAliasWithoutRandomGeneration() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://example.com/page");
        request.setCustomAlias("my_alias");
        request.setExpiresAt(LocalDateTime.now().plusDays(1));

        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(shortUrlRepository.existsByCustomAlias("my_alias")).thenReturn(false);
        when(shortUrlRepository.existsByShortCode("my_alias")).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrlEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(urlService.create(request).getShortCode()).isEqualTo("my_alias");
        verify(shortCodeGenerator, never()).generate();
    }

    @Test
    void createShouldRetryShortCodeCollision() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://example.com/page");
        request.setExpiresAt(LocalDateTime.now().plusDays(1));

        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(shortCodeGenerator.generate()).thenReturn("COLLIDE", "UNIQUE1");
        when(shortUrlRepository.existsByShortCode("COLLIDE")).thenReturn(true);
        when(shortUrlRepository.existsByShortCode("UNIQUE1")).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrlEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(urlService.create(request).getShortCode()).isEqualTo("UNIQUE1");
        verify(metrics).shortCodeGeneration("collision_retry");
        verify(metrics).shortCodeGeneration("success");
    }

    @Test
    void createShouldThrowControlledExceptionWhenShortCodeRetriesAreExhausted() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://example.com/page");
        request.setExpiresAt(LocalDateTime.now().plusDays(1));

        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(shortCodeGenerator.generate()).thenReturn("COLLIDE");
        when(shortUrlRepository.existsByShortCode("COLLIDE")).thenReturn(true);

        assertThatThrownBy(() -> urlService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unable to generate unique short code");
        verify(metrics, times(5)).shortCodeGeneration("collision_retry");
        verify(metrics).shortCodeGeneration("retry_exhausted");
    }

    @Test
    void getShouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(shortUrlRepository.findByIdAndWorkspaceId(id, workspaceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.get(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getShouldReturnOwnedUrl() {
        UUID id = UUID.randomUUID();
        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        ShortUrlEntity entity = new ShortUrlEntity(id, "ABC1234", "https://example.com", null, owner, workspace, LocalDateTime.now().plusDays(1));
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(shortUrlRepository.findByIdAndWorkspaceId(id, workspaceId)).thenReturn(Optional.of(entity));

        assertThat(urlService.get(id).getId()).isEqualTo(id);
    }

    @Test
    void getShouldDenyAccessToAnotherOwnersUrl() {
        UUID id = UUID.randomUUID();
        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(shortUrlRepository.findByIdAndWorkspaceId(id, workspaceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.get(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(shortUrlRepository, never()).findById(id);
    }

    @Test
    void listShouldReturnPagedResponse() {
        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);

        ShortUrlEntity entity = new ShortUrlEntity(UUID.randomUUID(), "ABC1234", "https://example.com", null, owner, workspace, LocalDateTime.now().plusDays(1));
        when(shortUrlRepository.findByWorkspaceId(workspaceId, PageRequest.of(0, 20))).thenReturn(new PageImpl<>(Collections.singletonList(entity)));

        Page<?> page = urlService.list(0, 20);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void listShouldUseCurrentOwnerAndRequestedPagination() {
        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(shortUrlRepository.findByWorkspaceId(workspaceId, PageRequest.of(2, 5))).thenReturn(Page.empty());

        Page<?> page = urlService.list(2, 5);

        assertThat(page.getContent()).isEmpty();
        verify(shortUrlRepository).findByWorkspaceId(workspaceId, PageRequest.of(2, 5));
    }

    @Test
    void updateExpirationShouldUpdateOwnedUrl() {
        UUID id = UUID.randomUUID();
        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        ShortUrlEntity entity = new ShortUrlEntity(id, "ABC1234", "https://example.com", null, owner, workspace, LocalDateTime.now().plusDays(1));
        UpdateShortUrlRequest request = new UpdateShortUrlRequest();
        request.setExpiresAt(LocalDateTime.now().plusDays(3));
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(shortUrlRepository.findByIdAndWorkspaceId(id, workspaceId)).thenReturn(Optional.of(entity));
        when(shortUrlRepository.save(entity)).thenReturn(entity);

        assertThat(urlService.updateExpiration(id, request).getExpiresAt()).isEqualTo(request.getExpiresAt());
    }

    @Test
    void updateDestinationShouldRequireMatchingEntityVersionAndInvalidateCache() {
        UUID id = UUID.randomUUID();
        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        ShortUrlEntity entity = new ShortUrlEntity(id, "ABC1234", "https://example.com/old", null, owner, workspace, LocalDateTime.now().plusDays(1));
        UpdateDestinationRequest request = new UpdateDestinationRequest();
        request.setOriginalUrl("https://example.com/new");
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(shortUrlRepository.findByIdAndWorkspaceId(id, workspaceId)).thenReturn(Optional.of(entity));
        when(shortUrlRepository.save(entity)).thenReturn(entity);

        ShortUrlResponse response = urlService.updateDestination(id, request, entity.getVersion());

        assertThat(response.getOriginalUrl()).isEqualTo("https://example.com/new");
        assertThat(entity.getShortCode()).isEqualTo("ABC1234");
        verify(validationService).validateOriginalUrl("https://example.com/new");
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void updateDestinationShouldRejectStaleVersion() {
        UUID id = UUID.randomUUID();
        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        ShortUrlEntity entity = new ShortUrlEntity(id, "ABC1234", "https://example.com/old", null, owner, workspace, LocalDateTime.now().plusDays(1));
        UpdateDestinationRequest request = new UpdateDestinationRequest();
        request.setOriginalUrl("https://example.com/new");
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(shortUrlRepository.findByIdAndWorkspaceId(id, workspaceId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> urlService.updateDestination(id, request, entity.getVersion() + 1))
                .isInstanceOf(PreconditionFailedException.class);
        verify(shortUrlRepository, never()).save(entity);
    }

    @Test
    void disableAndEnableShouldChangeOwnedUrlState() {
        UUID id = UUID.randomUUID();
        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        ShortUrlEntity entity = new ShortUrlEntity(id, "ABC1234", "https://example.com", null, owner, workspace, LocalDateTime.now().plusDays(1));
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(shortUrlRepository.findByIdAndWorkspaceId(id, workspaceId)).thenReturn(Optional.of(entity));
        when(shortUrlRepository.save(entity)).thenReturn(entity);

        assertThat(urlService.disable(id).isEnabled()).isFalse();
        assertThat(urlService.enable(id).isEnabled()).isTrue();
    }

    @Test
    void ownerShouldNotEnableAdministrativelyBlockedUrl() {
        UUID id = UUID.randomUUID();
        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        ShortUrlEntity entity = new ShortUrlEntity(id, "ABC1234", "https://example.com", null, owner, workspace, LocalDateTime.now().plusDays(1));
        entity.setBlocked(true);
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(shortUrlRepository.findByIdAndWorkspaceId(id, workspaceId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> urlService.enable(id))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Blocked");
    }

    @Test
    void adminBlockAndUnblockShouldBeIdempotentAndInvalidateCacheOnlyOnChange() {
        UUID id = UUID.randomUUID();
        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        ShortUrlEntity entity = new ShortUrlEntity(id, "ABC1234", "https://example.com", null, owner, workspace, LocalDateTime.now().plusDays(1));
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(UUID.randomUUID(), "admin@example.com", UserRole.ADMIN));
        when(shortUrlRepository.findById(id)).thenReturn(Optional.of(entity));
        when(shortUrlRepository.save(entity)).thenReturn(entity);

        urlService.block(id);
        urlService.block(id);
        urlService.unblock(id);
        urlService.unblock(id);

        assertThat(entity.isBlocked()).isFalse();
        verify(shortUrlRepository, times(2)).save(entity);
        verify(eventPublisher, times(2)).publishEvent(any(Object.class));
    }

    @Test
    void deleteShouldSoftDeleteOnlyOwnedUrl() {
        UUID id = UUID.randomUUID();
        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        ShortUrlEntity entity = new ShortUrlEntity(id, "ABC1234", "https://example.com", null, owner, workspace, LocalDateTime.now().plusDays(1));
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(shortUrlRepository.findByIdAndWorkspaceId(id, workspaceId)).thenReturn(Optional.of(entity));

        urlService.delete(id);

        assertThat(entity.isDeleted()).isTrue();
        assertThat(entity.isEnabled()).isFalse();
        verify(shortUrlRepository).save(entity);
        verify(shortUrlRepository, never()).deleteById(id);
    }

    @Test
    void deleteShouldRejectMissingOrUnownedUrl() {
        UUID id = UUID.randomUUID();
        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(shortUrlRepository.findByIdAndWorkspaceId(id, workspaceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(shortUrlRepository, never()).deleteById(id);
    }

    @Test
    void updateExpirationShouldThrowWhenExpiredDateIsInPast() {
        UserEntity owner = new UserEntity(UUID.randomUUID(), PLACEHOLDER_EMAIL, "", UserRole.USER, UserStatus.ACTIVE);
        UUID id = UUID.randomUUID();
        UpdateShortUrlRequest request = new UpdateShortUrlRequest();
        request.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(ownerProvider.getCurrentOwner()).thenReturn(new OwnerIdentity(owner.getId(), PLACEHOLDER_EMAIL, UserRole.USER));
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        Mockito.doThrow(new BadRequestException("expiresAt must be a future timestamp"))
                .when(validationService).validateExpiration(null, request.getExpiresAt());

        assertThatThrownBy(() -> urlService.updateExpiration(id, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expiresAt must be a future timestamp");
    }
}

