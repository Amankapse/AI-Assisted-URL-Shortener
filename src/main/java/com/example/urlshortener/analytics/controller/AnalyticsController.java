package com.example.urlshortener.analytics.controller;

import com.example.urlshortener.analytics.dto.AnalyticsDtos.AdminAnalyticsOverviewResponse;
import com.example.urlshortener.analytics.dto.AnalyticsDtos.DailyRedirectsResponse;
import com.example.urlshortener.analytics.dto.AnalyticsDtos.TopLinkResponse;
import com.example.urlshortener.analytics.dto.AnalyticsDtos.UrlAnalyticsResponse;
import com.example.urlshortener.analytics.service.AnalyticsQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class AnalyticsController {
    private final AnalyticsQueryService analyticsQueryService;

    public AnalyticsController(AnalyticsQueryService analyticsQueryService) {
        this.analyticsQueryService = analyticsQueryService;
    }

    @GetMapping("/api/v1/urls/{id}/analytics")
    public ResponseEntity<UrlAnalyticsResponse> urlAnalytics(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(analyticsQueryService.urlAnalytics(id));
    }

    @GetMapping("/api/v1/urls/{id}/analytics/daily")
    public ResponseEntity<DailyRedirectsResponse> dailyAnalytics(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(analyticsQueryService.dailyAnalytics(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/analytics/overview")
    public ResponseEntity<AdminAnalyticsOverviewResponse> adminOverview() {
        return ResponseEntity.ok(analyticsQueryService.adminOverview());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/analytics/top-links")
    public ResponseEntity<List<TopLinkResponse>> topLinks(@RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsQueryService.topLinks(limit));
    }
}
