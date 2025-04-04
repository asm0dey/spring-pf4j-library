package com.github.asm0dey.opdsko_spring.meilisearch;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

import java.net.InetSocketAddress;
import java.util.Optional;

public class MeilisearchDockerComposeConnectionDetailsFactory
        extends DockerComposeConnectionDetailsFactory<MeilisearchConnectionDetails> {

    public MeilisearchDockerComposeConnectionDetailsFactory() {
        super("getmeili/meilisearch", "com.meilisearch.sdk.Client");
    }

    @Override
    public MeilisearchConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
        return new MeilisearchDockerComposeConnectionDetails(source.getRunningService());
    }

    public static class MeilisearchDockerComposeConnectionDetails
            extends DockerComposeConnectionDetails
            implements MeilisearchConnectionDetails {

        private final RunningService runningService;

        MeilisearchDockerComposeConnectionDetails(RunningService runningService) {
            super(runningService);
            this.runningService = runningService;
        }

        @Override
        public String address() {
            return Optional.ofNullable(runningService.host()).orElse("localhost") + ":" + runningService.ports().get(7700);
        }

        @Override
        public String key() {
            return runningService.env().get("MEILI_MASTER_KEY");
        }
    }
}