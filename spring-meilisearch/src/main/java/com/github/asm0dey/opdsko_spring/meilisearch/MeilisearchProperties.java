package com.github.asm0dey.opdsko_spring.meilisearch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meilisearch")
public record MeilisearchProperties(String apiKey, String host, int port) {
    public MeilisearchProperties {
        if (host == null) {
            host = "";
        }
        if (apiKey == null) {
            apiKey = "";
        }
    }
}
