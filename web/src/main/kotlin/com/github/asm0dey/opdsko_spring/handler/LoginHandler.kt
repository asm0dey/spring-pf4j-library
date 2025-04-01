package com.github.asm0dey.opdsko_spring.handler

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait

/**
 * Handler for the login page.
 * Provides a simple login form for form-based authentication.
 */
@Component
class LoginHandler {

    /**
     * Renders the login page.
     * The login form submits to /login which is handled by Spring Security.
     *
     * @param request The server request
     * @return The server response with the login page HTML
     */
    suspend fun loginPage(request: ServerRequest): ServerResponse {
        val error = request.queryParam("error").isPresent
        val html = createHTML().html {
            head {
                title("Login")
                meta(charset = "utf-8")
                meta(name = "viewport", content = "width=device-width, initial-scale=1")
                link(rel = "stylesheet", href = "/webjars/spectre.css/0.5.9/dist/spectre.min.css")
                link(rel = "stylesheet", href = "/webjars/spectre.css/0.5.9/dist/spectre-icons.min.css")
                style {
                    +"""
                    .container {
                        max-width: 400px;
                        margin: 0 auto;
                        padding: 2rem;
                    }
                    .form-error {
                        color: #e85600;
                        margin-bottom: 1rem;
                    }
                    .oauth-button {
                        margin-top: 1rem;
                    }
                    """
                }
                link(href = "/apple-touch-icon.png", rel = "apple-touch-icon") {
                    sizes = "180x180"
                }
                link(href = "/favicon-32x32.png", rel = "icon", type = "image/png") {
                    sizes = "32x32"
                }
                link(href = "/favicon-16x16.png", rel = "icon", type = "image/png") {
                    sizes = "16x16"
                }
                link(rel = "manifest", href = "/site.webmanifest")
                link(rel = "mask-icon", href = "/safari-pinned-tab.svg") {
                    attributes["color"] = "#5bbad5"
                }
                meta(name = "msapplication-TileColor", content = "#2b5797")
                meta(name = "theme-color", content = "#ffffff")
            }
            body {
                div(classes = "container") {
                    h1 { +"Login" }
                    if (error) {
                        div(classes = "form-error toast toast-error") {
                            +"Invalid email or password"
                        }
                    }
                    form(action = "/login", method = FormMethod.post) {
                        div(classes = "form-group") {
                            label(classes = "form-label") {
                                htmlFor = "username"
                                +"Email"
                            }
                            input(type = InputType.email, name = "username", classes = "form-input") {
                                attributes["id"] = "username"
                                required = true
                                autoFocus = true
                            }
                        }
                        div(classes = "form-group") {
                            label(classes = "form-label") {
                                htmlFor = "password"
                                +"Password"
                            }
                            input(type = InputType.password, name = "password", classes = "form-input") {
                                attributes["id"] = "password"
                                required = true
                            }
                        }
                        button(type = ButtonType.submit, classes = "btn btn-primary") {
                            +"Login"
                        }
                    }
                    div(classes = "oauth-button") {
                        a(href = "/oauth2/authorization/google", classes = "btn btn-link") {
                            +"Login with Google"
                        }
                    }
                }
            }
        }
        return ServerResponse.ok()
            .contentType(MediaType.TEXT_HTML)
            .bodyValueAndAwait(html)
    }
}
