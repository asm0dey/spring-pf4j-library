package com.github.asm0dey.opdsko_spring.meilisearch;

import java.net.InetSocketAddress;

public class MeilisearchPropertiesConnectionDetails implements MeilisearchConnectionDetails {
    private final MeilisearchProperties properties;

    public MeilisearchPropertiesConnectionDetails(MeilisearchProperties properties) {
        this.properties = properties;
    }

    @Override
    public InetSocketAddress address() {
        if (properties.host().isBlank()) throw new IllegalStateException("Meilisearch host is blank");
        return new InetSocketAddress(
            properties.host(),
            properties.port()
        );
    }

    @Override
    public String key() {
        return properties.apiKey();
    }
}
