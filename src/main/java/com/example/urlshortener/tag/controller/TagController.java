package com.example.urlshortener.tag.controller;

import com.example.urlshortener.tag.dto.TagResponse;
import com.example.urlshortener.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Tags")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/tags")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @Operation(summary = "List workspace tags")
    @GetMapping
    public List<TagResponse> list(@PathVariable("workspaceId") UUID workspaceId) {
        return tagService.list(workspaceId);
    }
}
