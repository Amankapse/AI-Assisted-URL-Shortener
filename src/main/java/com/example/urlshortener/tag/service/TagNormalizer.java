package com.example.urlshortener.tag.service;

import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.url.config.UrlOrganizationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class TagNormalizer {
    private static final Pattern SAFE_TAG = Pattern.compile("^[a-z0-9][a-z0-9_-]{0,49}$");

    private final UrlOrganizationProperties properties;

    public TagNormalizer(UrlOrganizationProperties properties) {
        this.properties = properties;
    }

    public List<String> normalize(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null || tag.isBlank()) {
                throw new BadRequestException("tags must not contain blank values");
            }
            String value = tag.trim().toLowerCase(Locale.ROOT);
            if (value.length() > properties.getTagMaxLength()) {
                throw new BadRequestException("tag may not exceed " + properties.getTagMaxLength() + " characters");
            }
            if (!SAFE_TAG.matcher(value).matches()) {
                throw new BadRequestException("tag may contain only lowercase letters, digits, hyphen, and underscore");
            }
            normalized.add(value);
        }
        if (normalized.size() > properties.getTagsPerUrl()) {
            throw new BadRequestException("too many tags for URL");
        }
        return normalized.stream().sorted().toList();
    }

    public String normalizeOne(String tag) {
        List<String> normalized = normalize(new ArrayList<>(List.of(tag)));
        return normalized.getFirst();
    }
}
