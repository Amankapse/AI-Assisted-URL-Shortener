package com.example.urlshortener.site.service;

import com.example.urlshortener.audit.entity.AuditAction;
import com.example.urlshortener.audit.entity.AuditResourceType;
import com.example.urlshortener.audit.service.AuditService;
import com.example.urlshortener.site.config.AdminBootstrapProperties;
import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.entity.UserRole;
import com.example.urlshortener.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Component
public class AdminBootstrapService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapService.class);

    private final AdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public AdminBootstrapService(AdminBootstrapProperties properties, UserRepository userRepository, AuditService auditService) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Override
    public void run(ApplicationArguments args) {
        promoteConfiguredAdmin();
    }

    @Transactional
    public BootstrapResult promoteConfiguredAdmin() {
        String email = normalize(properties.getAdminEmail());
        if (email == null) {
            return BootstrapResult.NOOP_ABSENT;
        }
        UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.warn("Admin bootstrap email did not match an existing registered user; no administrator was created.");
            return BootstrapResult.NOOP_UNKNOWN_USER;
        }
        if (user.getRole() == UserRole.ADMIN) {
            return BootstrapResult.NOOP_ALREADY_ADMIN;
        }
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);
        auditService.recordSystem(AuditAction.ADMIN_PROMOTED, null, AuditResourceType.USER, user.getId(),
                Map.of("method", "APP_BOOTSTRAP_ADMIN_EMAIL"));
        log.info("Admin bootstrap promoted one existing registered user.");
        return BootstrapResult.PROMOTED;
    }

    private String normalize(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            log.warn("Admin bootstrap email is not valid; no administrator was created.");
            return null;
        }
        return normalized;
    }

    public enum BootstrapResult {
        NOOP_ABSENT,
        NOOP_UNKNOWN_USER,
        NOOP_ALREADY_ADMIN,
        PROMOTED
    }
}
