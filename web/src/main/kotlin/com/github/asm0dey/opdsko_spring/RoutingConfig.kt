package com.github.asm0dey.opdsko_spring

import com.github.asm0dey.opdsko_spring.handler.HtmxHandler
import com.github.asm0dey.opdsko_spring.handler.LoginHandler
import com.github.asm0dey.opdsko_spring.handler.OpdsHandler
import com.github.asm0dey.opdsko_spring.handler.SimpleHandler
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
    fun router(
        htmxHandler: HtmxHandler,
        opdsHandler: OpdsHandler,
        commonHandler: CommonHandler,
        scanner: Scanner,
        simpleHandler: SimpleHandler,
        loginHandler: LoginHandler
    ) = coRouter {
        GET("/") { ServerResponse.permanentRedirect(URI("/api")).buildAndAwait() }
        resources("/**", ClassPathResource("static/"))
        GET("/login", loginHandler::loginPage)
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

        // Simple UI routes (no HTMX, using pico.css)
        GET("/simple").nest {
            GET("", simpleHandler::homePage)
            GET("/search", simpleHandler::search)
            GET("/new", simpleHandler::new)

            GET("/author").nest {
                GET("", simpleHandler::authorFirstLevel)
                GET("/{prefix}", simpleHandler::authorPrefixLevel)
                GET("/view").nest {
                    GET("/{fullName}").nest {
                        GET("", simpleHandler::authorView)
                        GET("/series", simpleHandler::authorSeries)
                        GET("/series/{series}", simpleHandler::authorSeriesBooks)
                        GET("/noseries", simpleHandler::authorNoSeriesBooks)
                        GET("/all", simpleHandler::authorAllBooks)
                    }
                }
            }
            GET("/series/{series}", simpleHandler::seriesBooks)

            // Book info and full image pages (replacing modals)
            GET("/book/{id}").nest {
                GET("/info", simpleHandler::getBookInfo)
                GET("/fullimage", simpleHandler::getFullBookCover)
            }
        }

        // OPDS routes
        GET("/opds").nest {
            GET("", opdsHandler::homePage)
            GET("/search", opdsHandler::search)
            GET("/new", opdsHandler::new)

            GET("/author").nest {
                GET("", opdsHandler::authorFirstLevel)
                GET("/{prefix}", opdsHandler::authorPrefixLevel)
                GET("/view").nest {
                    GET("/{fullName}").nest {
                        GET("", opdsHandler::authorView)
                        GET("/series", opdsHandler::authorSeries)
                        GET("/series/{series}", opdsHandler::authorSeriesBooks)
                        GET("/noseries", opdsHandler::authorNoSeriesBooks)
                        GET("/all", opdsHandler::authorAllBooks)
                    }
                }
            }
            GET("/series/{series}", opdsHandler::seriesBooks)
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
