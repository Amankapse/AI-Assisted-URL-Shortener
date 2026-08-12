package com.example.urlshortener.site;

import com.example.urlshortener.analytics.repository.ClickEventRepository;
import com.example.urlshortener.audit.entity.AuditAction;
import com.example.urlshortener.audit.repository.AuditRepository;
import com.example.urlshortener.auth.repository.RefreshTokenRepository;
import com.example.urlshortener.site.config.AdminBootstrapProperties;
import com.example.urlshortener.site.entity.ContentPageKey;
import com.example.urlshortener.site.entity.ContentStatus;
import com.example.urlshortener.site.entity.SiteSettingsEntity;
import com.example.urlshortener.site.repository.AnnouncementRepository;
import com.example.urlshortener.site.repository.ContentPageRepository;
import com.example.urlshortener.site.repository.MediaAssetRepository;
import com.example.urlshortener.site.repository.SiteSettingsRepository;
import com.example.urlshortener.site.service.AdminBootstrapService;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.entity.UserRole;
import com.example.urlshortener.user.entity.UserStatus;
import com.example.urlshortener.user.repository.UserRepository;
import com.example.urlshortener.workspace.repository.WorkspaceMembershipRepository;
import com.example.urlshortener.workspace.repository.WorkspaceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SiteExperienceIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ShortUrlRepository shortUrlRepository;
    @Autowired ClickEventRepository clickEventRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired AuditRepository auditRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMembershipRepository membershipRepository;
    @Autowired SiteSettingsRepository settingsRepository;
    @Autowired ContentPageRepository contentPageRepository;
    @Autowired AnnouncementRepository announcementRepository;
    @Autowired MediaAssetRepository mediaAssetRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AdminBootstrapProperties bootstrapProperties;
    @Autowired AdminBootstrapService bootstrapService;

    @BeforeEach
    void cleanDatabase() {
        SiteSettingsEntity settings = settingsRepository.findById(SiteSettingsEntity.SINGLETON_ID).orElseThrow();
        settings.setLogoAssetId(null);
        settings.setLogoDarkAssetId(null);
        settings.setFaviconAssetId(null);
        settings.setLoginBackgroundAssetId(null);
        settings.setLandingHeroAssetId(null);
        settings.setUpdatedBy(null);
        settings.setBrandName("Shortener Ops");
        settings.setTagline("Secure link management for teams and workspaces.");
        settings.setSupportEmail("support@example.com");
        settings.setSupportUrl("https://example.com/help");
        settings.setContactText("Demo support.");
        settings.setPrimaryColor("#185A9D");
        settings.setSecondaryColor("#123047");
        settings.setAccentColor("#0F766E");
        settings.setFooterDescription("Production-oriented URL shortener prototype.");
        settings.setFooterCopyright("Copyright 2026 Shortener Ops demo.");
        settingsRepository.saveAndFlush(settings);
        var pages = contentPageRepository.findAll();
        pages.forEach(page -> {
            page.setUpdatedBy(null);
            page.setStatus(ContentStatus.PUBLISHED);
            page.setPublishedAt(LocalDateTime.now());
        });
        contentPageRepository.saveAllAndFlush(pages);
        announcementRepository.deleteAll();
        mediaAssetRepository.deleteAll();
        auditRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        clickEventRepository.deleteAll();
        shortUrlRepository.deleteAll();
        membershipRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
        bootstrapProperties.setAdminEmail(null);
    }

    @Test
    void publicSiteApisExposeOnlyPublishedSafeData() throws Exception {
        mockMvc.perform(get("/api/v1/site/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brandName").value("Shortener Ops"))
                .andExpect(jsonPath("$.updatedBy").doesNotExist());

        mockMvc.perform(get("/api/v1/site/pages/SECURITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageKey").value("SECURITY"))
                .andExpect(content().string(not(containsString("private-key"))));

        mockMvc.perform(get("/api/v1/site/pages/ABOUT"))
                .andExpect(status().isOk());
    }

    @Test
    void adminCmsRequiresPlatformAdminAndUsesVersionChecks() throws Exception {
        String userToken = registerAndLogin("cms-user@example.com");
        String adminToken = createPlatformAdminAndLogin("cms-admin@example.com");

        mockMvc.perform(get("/api/v1/admin/site/settings").header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isForbidden());

        MvcResult result = mockMvc.perform(get("/api/v1/admin/site/settings").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();
        String etag = result.getResponse().getHeader(HttpHeaders.ETAG);

        mockMvc.perform(put("/api/v1/admin/site/settings")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(settingsBody("#FFFFFF"))))
                .andExpect(status().isPreconditionRequired());

        mockMvc.perform(put("/api/v1/admin/site/settings")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .header(HttpHeaders.IF_MATCH, etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(settingsBody("#FFFFFF"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("contrast")));

        mockMvc.perform(put("/api/v1/admin/site/settings")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .header(HttpHeaders.IF_MATCH, etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(settingsBody("#174C8C"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brandName").value("Ops Portal"))
                .andExpect(header().exists(HttpHeaders.ETAG));

        assertThat(auditRepository.countByAction(AuditAction.SITE_SETTINGS_UPDATED)).isEqualTo(1);
    }

    @Test
    void contentAnnouncementsAndMediaAreBoundedAndAudited() throws Exception {
        String adminToken = createPlatformAdminAndLogin("content-admin@example.com");
        mockMvc.perform(get("/api/v1/admin/site/content").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pageKey").exists());
        long aboutVersion = contentPageRepository.findByPageKey(ContentPageKey.ABOUT).orElseThrow().getVersion();

        mockMvc.perform(put("/api/v1/admin/site/content/ABOUT")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .header(HttpHeaders.IF_MATCH, "\"" + aboutVersion + "\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "About the platform",
                                "summary", "Updated summary",
                                "content", "Plain text only. No scripts are accepted.",
                                "status", "PUBLISHED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(post("/api/v1/admin/site/media")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Unsafe image",
                                "url", "http://example.com/image.png",
                                "altText", "unsafe",
                                "enabled", true))))
                .andExpect(status().isBadRequest());

        JsonNode media = objectMapper.readTree(mockMvc.perform(post("/api/v1/admin/site/media")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Hero",
                                "url", "https://example.com/hero.png",
                                "altText", "Abstract dashboard surface",
                                "sourceName", "Example",
                                "sourceUrl", "https://example.com/source",
                                "license", "Demo metadata",
                                "attribution", "Example",
                                "enabled", true))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/v1/admin/site/announcements")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Maintenance window",
                                "message", "Brief scheduled maintenance notice.",
                                "severity", "MAINTENANCE",
                                "audience", "PUBLIC",
                                "enabled", true,
                                "dismissible", true))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/site/announcements?audience=PUBLIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Maintenance window"));

        assertThat(media.get("url").asText()).isEqualTo("https://example.com/hero.png");
        assertThat(auditRepository.countByAction(AuditAction.CONTENT_PAGE_PUBLISHED)).isEqualTo(1);
        assertThat(auditRepository.countByAction(AuditAction.MEDIA_ASSET_REGISTERED)).isEqualTo(1);
        assertThat(auditRepository.countByAction(AuditAction.ANNOUNCEMENT_CREATED)).isEqualTo(1);
    }

    @Test
    void adminBootstrapIsSafeIdempotentAndDoesNotCreateUsers() throws Exception {
        assertThat(bootstrapService.promoteConfiguredAdmin()).isEqualTo(AdminBootstrapService.BootstrapResult.NOOP_ABSENT);

        bootstrapProperties.setAdminEmail("missing@example.com");
        assertThat(bootstrapService.promoteConfiguredAdmin()).isEqualTo(AdminBootstrapService.BootstrapResult.NOOP_UNKNOWN_USER);
        assertThat(userRepository.findByEmail("missing@example.com")).isEmpty();

        UserEntity user = saveUser("bootstrap@example.com", UserRole.USER);
        bootstrapProperties.setAdminEmail(" Bootstrap@Example.COM ");
        assertThat(bootstrapService.promoteConfiguredAdmin()).isEqualTo(AdminBootstrapService.BootstrapResult.PROMOTED);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(auditRepository.countByAction(AuditAction.ADMIN_PROMOTED)).isEqualTo(1);

        assertThat(bootstrapService.promoteConfiguredAdmin()).isEqualTo(AdminBootstrapService.BootstrapResult.NOOP_ALREADY_ADMIN);
        assertThat(auditRepository.countByAction(AuditAction.ADMIN_PROMOTED)).isEqualTo(1);
    }

    @Test
    void registrationAndWorkspaceAdministrationDoNotCreatePlatformAdmins() throws Exception {
        register("normal@example.com");
        assertThat(userRepository.findByEmail("normal@example.com").orElseThrow().getRole()).isEqualTo(UserRole.USER);

        UserEntity workspaceAdmin = saveUser("workspace-admin-only@example.com", UserRole.USER);
        assertThat(workspaceAdmin.getRole()).isEqualTo(UserRole.USER);
    }

    private String registerAndLogin(String email) throws Exception {
        register(email);
        return login(email);
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "correct-horse-password"))))
                .andExpect(status().isCreated());
    }

    private String createPlatformAdminAndLogin(String email) throws Exception {
        saveUser(email, UserRole.ADMIN);
        return login(email);
    }

    private UserEntity saveUser(String email, UserRole role) {
        return userRepository.saveAndFlush(new UserEntity(UUID.randomUUID(), email, passwordEncoder.encode("correct-horse-password"), role, UserStatus.ACTIVE));
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "correct-horse-password"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(response).get("accessToken").asText();
    }

    private Map<String, Object> settingsBody(String primaryColor) {
        return Map.ofEntries(
                Map.entry("brandName", "Ops Portal"),
                Map.entry("tagline", "Secure links for teams."),
                Map.entry("supportEmail", "support@example.com"),
                Map.entry("supportUrl", "https://example.com/help"),
                Map.entry("contactText", "Contact support through approved project channels."),
                Map.entry("primaryColor", primaryColor),
                Map.entry("secondaryColor", "#123047"),
                Map.entry("accentColor", "#0F766E"),
                Map.entry("footerDescription", "A secure link management portal."),
                Map.entry("footerCopyright", "Copyright 2026 Ops Portal.")
        );
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
