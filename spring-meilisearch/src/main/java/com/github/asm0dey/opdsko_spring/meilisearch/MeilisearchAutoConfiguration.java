package com.github.asm0dey.opdsko_spring.meilisearch;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(Client.class)
@EnableConfigurationProperties(MeilisearchProperties.class)
public class MeilisearchAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MeilisearchConnectionDetails.class)
    public MeilisearchConnectionDetails memcachedConnectionDetails(MeilisearchProperties props) {
        return new MeilisearchPropertiesConnectionDetails(props);
    }

    @Bean
    public Client meiliSearch(MeilisearchConnectionDetails connectionDetails) {
        return new Client(new Config("http://" + connectionDetails.address(), connectionDetails.key()));
    }
}