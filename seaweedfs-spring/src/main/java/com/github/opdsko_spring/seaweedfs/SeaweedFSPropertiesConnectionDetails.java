package com.github.opdsko_spring.seaweedfs;

import java.net.InetSocketAddress;

public class SeaweedFSPropertiesConnectionDetails implements SeaweedFSConnectionDetails {
    private final SeaweedFSProperties properties;

    public SeaweedFSPropertiesConnectionDetails(SeaweedFSProperties properties) {
        this.properties = properties;
    }

    @Override
    public InetSocketAddress masterAddress() {
        if (properties.host().isBlank()) throw new IllegalArgumentException("SeaweedFS host must not be blank");
        return new InetSocketAddress(
                properties.host(),
                properties.port()
        );
    }

    @Override
    public InetSocketAddress filerAddress() {
        if (properties.host().isBlank()) throw new IllegalArgumentException("SeaweedFS host must not be blank");
        return new InetSocketAddress(
                properties.host(),
                properties.filerPort()
        );
    }

    @Override
    public InetSocketAddress filerGrpcAddress() {
        if (properties.host().isBlank()) throw new IllegalArgumentException("SeaweedFS host must not be blank");
        return new InetSocketAddress(
                properties.host(),
                properties.filerGrpcPort()
        );
    }
}