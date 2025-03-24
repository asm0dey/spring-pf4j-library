package com.github.opdsko_spring.seaweedfs;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import seaweedfs.client.FilerClient;

import java.net.InetSocketAddress;

public class SeaweedFSDockerComposeConnectionDetailsFactory
        extends DockerComposeConnectionDetailsFactory<SeaweedFSConnectionDetails> {

    public SeaweedFSDockerComposeConnectionDetailsFactory() {
        super("chrislusf/seaweedfs", FilerClient.class.getName());
    }

    @Override
    public SeaweedFSConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
        return new SeaweedFSDockerComposeConnectionDetails(source.getRunningService());
    }

    public static class SeaweedFSDockerComposeConnectionDetails
            extends DockerComposeConnectionDetails
            implements SeaweedFSConnectionDetails {

        private final RunningService runningService;

        SeaweedFSDockerComposeConnectionDetails(RunningService runningService) {
            super(runningService);
            this.runningService = runningService;
        }

        @Override
        public InetSocketAddress masterAddress() {
            return new InetSocketAddress(
                    runningService.host(),
                    runningService.ports().get(9333)
            );
        }

        @Override
        public InetSocketAddress filerAddress() {
            return new InetSocketAddress(
                    runningService.host(),
                    runningService.ports().get(8888)
            );
        }

        @Override
        public InetSocketAddress filerGrpcAddress() {
            return new InetSocketAddress(
                    runningService.host(),
                    runningService.ports().get(18888)
            );

        }
    }
}