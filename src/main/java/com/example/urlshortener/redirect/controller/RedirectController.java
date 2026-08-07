package com.example.urlshortener.redirect.controller;

import com.example.urlshortener.redirect.service.RedirectService;
import com.example.urlshortener.redirect.service.RedirectTarget;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Redirect")
@RestController
public class RedirectController {

    private final RedirectService redirectService;

    public RedirectController(RedirectService redirectService) {
        this.redirectService = redirectService;
    }

    @Operation(summary = "Redirect a short code to the original URL")
    @GetMapping("/r/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable("shortCode") String shortCode, HttpServletRequest request) {
        RedirectTarget entity = redirectService.resolve(shortCode, request);
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, entity.destinationUrl())
                .build();
    }
}
