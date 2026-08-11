package com.example.urlshortener.url.dto;

import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class UpdateUrlTagsRequest {
    @Size(max = 50, message = "tags list is too large")
    private List<String> tags = new ArrayList<>();

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }
}
