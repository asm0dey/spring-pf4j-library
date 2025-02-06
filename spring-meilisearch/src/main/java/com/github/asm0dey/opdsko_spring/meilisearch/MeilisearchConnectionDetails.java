package com.github.asm0dey.opdsko_spring.meilisearch;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

import java.net.InetSocketAddress;

public interface MeilisearchConnectionDetails extends ConnectionDetails {
    InetSocketAddress address();
    String key();
}
