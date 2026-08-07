package com.example.urlshortener.url.service;

import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.common.exception.ResourceNotFoundException;
import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.common.ratelimit.RateLimiterService;
import com.example.urlshortener.redirect.cache.RedirectCacheInvalidationEvent;
import com.example.urlshortener.redirect.service.RedirectTarget;
import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.dto.ShortUrlResponse;
import com.example.urlshortener.url.dto.UpdateShortUrlRequest;
import com.example.urlshortener.url.entity.ShortUrlEntity;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.repository.UserRepository;
import com.example.urlshortener.user.service.CurrentOwnerProvider;
import com.example.urlshortener.user.service.OwnerIdentity;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final UrlValidationService validationService;
    private final ShortCodeGenerator shortCodeGenerator;
    private final CurrentOwnerProvider currentOwnerProvider;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RateLimiterService rateLimiter;
    private final AppMetrics metrics;

    public UrlService(ShortUrlRepository shortUrlRepository,
                      UrlValidationService validationService,
                      ShortCodeGenerator shortCodeGenerator,
                      CurrentOwnerProvider currentOwnerProvider,
                      UserRepository userRepository,
                      ApplicationEventPublisher eventPublisher,
                      RateLimiterService rateLimiter,
                      AppMetrics metrics) {
        this.shortUrlRepository = shortUrlRepository;
        this.validationService = validationService;
        this.shortCodeGenerator = shortCodeGenerator;
        this.currentOwnerProvider = currentOwnerProvider;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
    }

    @Transactional
    public ShortUrlResponse create(CreateShortUrlRequest request) {
        OwnerIdentity identity = currentOwnerProvider.getCurrentOwner();
        rateLimiter.enforce("url-create", identity.userId().toString());
        UserEntity owner = ownerReference(identity);
        try {
            validationService.validateOriginalUrl(request.getOriginalUrl());
            validationService.validateCustomAlias(request.getCustomAlias());
            validationService.validateExpiration(request.getCustomAlias(), request.getExpiresAt());

            String shortCode = request.getCustomAlias();
            if (shortCode != null && !shortCode.isBlank()) {
                if (shortUrlRepository.existsByCustomAlias(shortCode) || shortUrlRepository.existsByShortCode(shortCode)) {
                    throw new BadRequestException("customAlias is already in use");
                }
            } else {
                shortCode = generateUniqueShortCode();
            }

            ShortUrlEntity entity = new ShortUrlEntity(UUID.randomUUID(), shortCode, request.getOriginalUrl(), request.getCustomAlias(), owner, request.getExpiresAt());
            ShortUrlEntity saved = shortUrlRepository.save(entity);
            metrics.urlCreated();
            return mapToResponse(saved);
        } catch (BadRequestException ex) {
            metrics.urlCreationFailed("customAlias is already in use".equals(ex.getMessage()) ? "alias_conflict" : "validation");
            throw ex;
        } catch (RuntimeException ex) {
            metrics.urlCreationFailed("error");
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public ShortUrlResponse get(UUID id) {
        UserEntity owner = ownerReference(currentOwnerProvider.getCurrentOwner());
        ShortUrlEntity entity = shortUrlRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public Page<ShortUrlResponse> list(int page, int size) {
        UserEntity owner = ownerReference(currentOwnerProvider.getCurrentOwner());
        Pageable pageable = PageRequest.of(page, size);
        return shortUrlRepository.findByOwner(owner, pageable).map(this::mapToResponse);
    }

    @Transactional
    public ShortUrlResponse updateExpiration(UUID id, UpdateShortUrlRequest request) {
        validationService.validateExpiration(null, request.getExpiresAt());
        UserEntity owner = ownerReference(currentOwnerProvider.getCurrentOwner());
        ShortUrlEntity entity = shortUrlRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        entity.setExpiresAt(request.getExpiresAt());
        ShortUrlResponse response = mapToResponse(shortUrlRepository.save(entity));
        eventPublisher.publishEvent(new RedirectCacheInvalidationEvent(entity.getShortCode()));
        return response;
    }

    @Transactional
    public void delete(UUID id) {
        UserEntity owner = ownerReference(currentOwnerProvider.getCurrentOwner());
        ShortUrlEntity entity = shortUrlRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        entity.setDeleted(true);
        entity.setEnabled(false);
        shortUrlRepository.save(entity);
        eventPublisher.publishEvent(new RedirectCacheInvalidationEvent(entity.getShortCode()));
    }

    @Transactional
    public ShortUrlResponse disable(UUID id) {
        UserEntity owner = ownerReference(currentOwnerProvider.getCurrentOwner());
        ShortUrlEntity entity = shortUrlRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        entity.setEnabled(false);
        ShortUrlResponse response = mapToResponse(shortUrlRepository.save(entity));
        eventPublisher.publishEvent(new RedirectCacheInvalidationEvent(entity.getShortCode()));
        return response;
    }

    @Transactional
    public ShortUrlResponse enable(UUID id) {
        UserEntity owner = ownerReference(currentOwnerProvider.getCurrentOwner());
        ShortUrlEntity entity = shortUrlRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        entity.setEnabled(true);
        ShortUrlResponse response = mapToResponse(shortUrlRepository.save(entity));
        eventPublisher.publishEvent(new RedirectCacheInvalidationEvent(entity.getShortCode()));
        return response;
    }

    @Transactional(readOnly = true)
    public ShortUrlEntity resolveByShortCode(String shortCode) {
        return shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
    }

    @Transactional(readOnly = true)
    public RedirectTarget resolveRedirectTarget(String shortCode) {
        return RedirectTarget.fromEntity(resolveByShortCode(shortCode));
    }

    private String generateUniqueShortCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = shortCodeGenerator.generate();
            if (!shortUrlRepository.existsByShortCode(candidate)) {
                return candidate;
            }
        }
        throw new BadRequestException("Unable to generate unique short code after retries");
    }

    private ShortUrlResponse mapToResponse(ShortUrlEntity entity) {
        return new ShortUrlResponse(
                entity.getId(),
                entity.getShortCode(),
                entity.getCustomAlias(),
                entity.getOriginalUrl(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.isEnabled(),
                entity.getClickCount()
        );
    }

    private UserEntity ownerReference(OwnerIdentity identity) {
        return userRepository.getReferenceById(identity.userId());
    }
}
