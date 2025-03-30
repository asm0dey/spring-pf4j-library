package com.github.asm0dey.opdsko_spring.meilisearch;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

public interface MeilisearchConnectionDetails extends ConnectionDetails {
    String address();

    String key();
}
