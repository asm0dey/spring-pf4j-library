package com.github.asm0dey.opdsko_spring.meilisearch.com.github.asm0dey.opdsko_spring.meilisearch

import com.github.asm0dey.opdsko_spring.meilisearch.MeilisearchConnectionDetails
import com.github.asm0dey.opdsko_spring.meilisearch.MeilisearchProperties
import com.github.asm0dey.opdsko_spring.meilisearch.MeilisearchPropertiesConnectionDetails
import com.meilisearch.sdk.Config
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import com.meilisearch.sdk.Client as MeilisearchClient

@AutoConfiguration
@ConditionalOnClass(MeilisearchClient::class)
@EnableConfigurationProperties(MeilisearchProperties::class)
class MeilisearchAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(MeilisearchConnectionDetails::class)
    fun memcachedConnectionDetails(props: MeilisearchProperties): MeilisearchConnectionDetails =
        MeilisearchPropertiesConnectionDetails(props)

    @Bean
    fun meiliSearch(connectionDetails: MeilisearchConnectionDetails) =
        MeilisearchClient(Config("http://${connectionDetails.address()}", connectionDetails.key()))
}