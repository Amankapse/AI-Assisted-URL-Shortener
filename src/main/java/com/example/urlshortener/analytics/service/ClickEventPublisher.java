package com.example.urlshortener.analytics.service;

import com.example.urlshortener.redirect.service.RedirectTarget;
import jakarta.servlet.http.HttpServletRequest;

public interface ClickEventPublisher {
    void publish(RedirectTarget target, HttpServletRequest request);
}
