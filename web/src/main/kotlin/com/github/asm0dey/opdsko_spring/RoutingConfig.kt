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
            GET("/new/{page}", htmxHandler::new)

            // Author navigation routes
            GET("/author", htmxHandler::authorFirstLevel)
            GET("/author/{prefix}", htmxHandler::authorPrefixLevel)
            GET("/author/view/{fullName}", htmxHandler::authorView)
            GET("/author/view/{fullName}/series", htmxHandler::authorSeries)
            GET("/author/view/{fullName}/series/{series}", htmxHandler::authorSeriesBooks)
            GET("/author/view/{fullName}/noseries", htmxHandler::authorNoSeriesBooks)
            GET("/author/view/{fullName}/all", htmxHandler::authorAllBooks)
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
    }
}