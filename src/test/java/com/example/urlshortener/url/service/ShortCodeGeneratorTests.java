package com.example.urlshortener.url.service;

import com.example.urlshortener.url.config.ShortCodeProperties;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShortCodeGeneratorTests {

    private final ShortCodeProperties properties = new ShortCodeProperties();
    private final ShortCodeGenerator generator = new ShortCodeGenerator(properties);

    @Test
    void generatedCodeShouldHaveConfiguredLengthAndBase62Characters() {
        String code = generator.generate();

        assertThat(code).hasSize(8);
        assertThat(code).matches("^[0-9A-Za-z]+$");
    }

    @Test
    void generatedCodeShouldUseConfiguredLength() {
        properties.setLength(9);

        assertThat(generator.generate()).hasSize(9);
    }

    @Test
    void configurationShouldRejectInvalidBounds() {
        assertThatThrownBy(() -> properties.setLength(6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("length");
        assertThatThrownBy(() -> properties.setLength(17))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("length");
        assertThatThrownBy(() -> properties.setMaxRetries(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-retries");
        assertThatThrownBy(() -> properties.setMaxRetries(101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-retries");
    }

    @Test
    void generatedCodesShouldBeUniqueAcrossMeaningfulSample() {
        Set<String> generated = new HashSet<>();

        for (int i = 0; i < 2_000; i++) {
            generated.add(generator.generate());
        }

        assertThat(generated).hasSize(2_000);
    }

    @Test
    void generatedCodeShouldNotExposeUuidLikeEntityIds() {
        String code = generator.generate();

        assertThat(code).doesNotContain("-");
        assertThat(code).doesNotMatch("^[0-9a-fA-F]{32}$");
    }
}
