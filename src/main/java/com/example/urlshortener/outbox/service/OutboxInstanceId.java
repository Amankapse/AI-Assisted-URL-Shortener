package com.example.urlshortener.outbox.service;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

@Component
public class OutboxInstanceId {
    private final String value;

    public OutboxInstanceId() {
        this.value = hostname() + "-" + UUID.randomUUID();
    }

    public String value() {
        return value;
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName().replaceAll("[^A-Za-z0-9._-]", "_");
        } catch (UnknownHostException ex) {
            return "unknown-host";
        }
    }
}
