package com.example.urlshortener.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
    @GetMapping({
            "/",
            "/login",
            "/register",
            "/app",
            "/app/**"
    })
    public String forwardToSpa() {
        return "forward:/index.html";
    }
}
