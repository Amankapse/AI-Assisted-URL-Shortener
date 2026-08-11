package com.example.urlshortener.audit;

import com.example.urlshortener.audit.config.AuditProperties;
import com.example.urlshortener.audit.entity.AuditAction;
import com.example.urlshortener.audit.entity.AuditResourceType;
import com.example.urlshortener.audit.repository.AuditRepository;
import com.example.urlshortener.audit.service.AuditService;
import com.example.urlshortener.common.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AuditServiceTests {
    @Test
    void shouldRejectOversizedAuditMetadataBeforePersisting() {
        AuditRepository repository = Mockito.mock(AuditRepository.class);
        AuditProperties properties = new AuditProperties();
        properties.setMetadataMaxBytes(16);
        AuditService service = new AuditService(repository, properties, new ObjectMapper(), Clock.systemUTC());

        assertThatThrownBy(() -> service.recordUser(
                AuditAction.URL_CREATED,
                UUID.randomUUID(),
                UUID.randomUUID(),
                AuditResourceType.URL,
                UUID.randomUUID(),
                Map.of("reason", "this-value-is-too-large")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Audit metadata exceeds configured maximum");

        verify(repository, never()).save(any());
    }

    @Test
    void destinationMetadataUsesHostsAndHashesInsteadOfRawUrls() {
        AuditProperties properties = new AuditProperties();
        AuditService service = new AuditService(Mockito.mock(AuditRepository.class), properties, new ObjectMapper(), Clock.systemUTC());

        Map<String, Object> metadata = service.destinationChangedMetadata(
                "https://example.com/path?secret=old",
                "https://example.org/new?secret=new");

        assertThat(metadata).containsEntry("previousHost", "example.com");
        assertThat(metadata).containsEntry("newHost", "example.org");
        assertThat(metadata.get("previousUrlHash")).isNotEqualTo(metadata.get("newUrlHash"));
        assertThat(metadata.toString()).doesNotContain("secret=old", "secret=new", "/path", "/new");
    }
}
