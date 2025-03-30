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
    fun router(htmxHandler: HtmxHandler, commonHandler: CommonHandler, scanner: Scanner) = coRouter {
        GET("/") { ServerResponse.permanentRedirect(URI("/api")).buildAndAwait() }
        resources("/**", ClassPathResource("static/"))
        GET("/api").nest {
            GET("", htmxHandler::homePage)
            GET("/search", htmxHandler::search)
            GET("/new", htmxHandler::new)

            GET("/author").nest {
                GET("", htmxHandler::authorFirstLevel)
                GET("/{prefix}", htmxHandler::authorPrefixLevel)
                GET("/view").nest {
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
        GET("/common").nest {
            GET("/book/{id}").nest {
                GET("/download", commonHandler::downloadBook)
                GET("/download/{format}", commonHandler::downloadBook)
                GET("/info", commonHandler::getBookInfo)
            }
            GET("/image/{id}", commonHandler::getBookCover)
            GET("/fullimage/{id}", commonHandler::getFullBookCover)
        }
        POST("/scan", scanner::scan)
        POST("/cleanup", scanner::cleanup)
        POST("/resync", scanner::resyncMeilisearch)
    }
}
