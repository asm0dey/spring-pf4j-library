package com.github.asm0dey.opdsko_spring

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.coRouter
import java.net.URI

@Configuration
class RoutingConfig {

    @Bean
    fun router(htmxHandler: HtmxHandler, scanner: Scanner) = coRouter {
        GET("/") { ServerResponse.permanentRedirect(URI("/api")).buildAndAwait() }
        resources("/**", ClassPathResource("static/"))
        GET("/api").nest {
            GET("", htmxHandler::homePage)
            GET("/search", htmxHandler::search)
            GET("/new", htmxHandler::new)

            GET("/author").nest {
                GET("", htmxHandler::authorFirstLevel)
                GET("/{prefix}", htmxHandler::authorPrefixLevel)
                GET("/view/").nest {
                    GET("/{fullName}").nest {
                        GET("", htmxHandler::authorView)
                        GET("/series", htmxHandler::authorSeries)
                        GET("/series/{series}", htmxHandler::authorSeriesBooks)
                        GET("/noseries", htmxHandler::authorNoSeriesBooks)
                        GET("/all", htmxHandler::authorAllBooks)
                    }
                }
            }
            GET("/series/{series}", htmxHandler::seriesBooks)
        }
        GET("/opds/book/{id}/download", htmxHandler::downloadBook)
        GET("/opds/book/{id}/download/{format}", htmxHandler::downloadBook)
        GET("/opds/image/{id}", htmxHandler::getBookCover)
        GET("/opds/fullimage/{id}", htmxHandler::getFullBookCover)
        GET("/api/book/{id}/image", htmxHandler::getFullBookCover)
        GET("/api/book/{id}/info", htmxHandler::getBookInfo)
        POST("/scan", scanner::scan)
        POST("/cleanup", scanner::cleanup)
        POST("/resync", scanner::resyncMeilisearch)
    }
}
