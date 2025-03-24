package com.github.opdsko_spring.seaweedfs;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import seaweedfs.client.FilerClient;

@AutoConfiguration
@ConditionalOnClass(FilerClient.class)
@EnableConfigurationProperties(SeaweedFSProperties.class)
public class SeaweedFSAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SeaweedFSConnectionDetails.class)
    public SeaweedFSConnectionDetails seaweedFSConnectionDetails(SeaweedFSProperties props) {
        return new SeaweedFSPropertiesConnectionDetails(props);
    }

    @Bean
    public FilerClient seaweedFSFilerClient(SeaweedFSConnectionDetails connectionDetails) {
        return new FilerClient(
                connectionDetails.filerAddress().getHostString(),
                connectionDetails.filerAddress().getPort(),
                connectionDetails.filerGrpcAddress().getPort()
        );
    }
}