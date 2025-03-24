package com.github.opdsko_spring.seaweedfs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seaweedfs")
public record SeaweedFSProperties(String host, int port, int filerPort, int filerGrpcPort) {
    public SeaweedFSProperties {
        if (host == null) {
            host = "";
        }
    }
}