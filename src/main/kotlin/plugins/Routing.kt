package pl.wiktorlacki.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.*
import pl.wiktorlacki.routes.searchRoutes

fun Application.configureRouting() {
    routing {
        staticResources("/", "static")
        searchRoutes()
    }
}
