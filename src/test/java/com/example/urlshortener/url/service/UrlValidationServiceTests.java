package com.example.urlshortener.url.service;

import com.example.urlshortener.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlValidationServiceTests {

    private final UrlValidationService validationService = new UrlValidationService();

    @Test
    void shouldAcceptHttpAndHttpsUrls() {
        assertThatCode(() -> validationService.validateOriginalUrl("http://example.com/a"))
                .doesNotThrowAnyException();
        assertThatCode(() -> validationService.validateOriginalUrl("https://example.com/a"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectMalformedBlankAndOversizedUrls() {
        assertInvalidUrl("https://exa mple.com");
        assertInvalidUrl(" ");
        assertInvalidUrl("https://example.com/" + "a".repeat(2049));
    }

    @Test
    void shouldRejectUnsupportedSchemes() {
        assertInvalidUrl("ftp://example.com/file");
        assertInvalidUrl("file:///etc/passwd");
        assertInvalidUrl("javascript:alert(1)");
    }

    @Test
    void shouldRejectLocalLoopbackPrivateAndLinkLocalHosts() {
        assertInvalidUrl("http://localhost/path");
        assertInvalidUrl("http://127.0.0.1/path");
        assertInvalidUrl("http://[::1]/path");
        assertInvalidUrl("http://10.0.0.5/path");
        assertInvalidUrl("http://172.16.0.5/path");
        assertInvalidUrl("http://172.31.0.5/path");
        assertInvalidUrl("http://192.168.1.10/path");
        assertInvalidUrl("http://169.254.10.20/path");
    }

    @Test
    void shouldValidateAliasCharactersReservedWordsAndLengthBoundaries() {
        assertThatCode(() -> validationService.validateCustomAlias("abc-DEF_123"))
                .doesNotThrowAnyException();
        assertThatCode(() -> validationService.validateCustomAlias("a".repeat(100)))
                .doesNotThrowAnyException();

        assertInvalidAlias("bad.alias");
        assertInvalidAlias("bad/alias");
        assertInvalidAlias("api");
        assertInvalidAlias("R");
        assertInvalidAlias("a".repeat(101));
    }

    @Test
    void shouldValidateExpiration() {
        assertThatCode(() -> validationService.validateExpiration(null, LocalDateTime.now().plusMinutes(5)))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> validationService.validateExpiration(null, LocalDateTime.now().minusSeconds(1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("future");
    }

    private void assertInvalidUrl(String url) {
        assertThatThrownBy(() -> validationService.validateOriginalUrl(url))
                .isInstanceOf(BadRequestException.class);
    }

    private void assertInvalidAlias(String alias) {
        assertThatThrownBy(() -> validationService.validateCustomAlias(alias))
                .isInstanceOf(BadRequestException.class);
    }
}
