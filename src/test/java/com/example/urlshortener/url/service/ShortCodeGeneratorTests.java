package com.example.urlshortener.url.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ShortCodeGeneratorTests {

    private final ShortCodeGenerator generator = new ShortCodeGenerator();

    @Test
    void generatedCodeShouldHaveConfiguredLengthAndBase62Characters() {
        String code = generator.generate();

        assertThat(code).hasSizeBetween(7, 8);
        assertThat(code).matches("^[0-9A-Za-z]+$");
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
