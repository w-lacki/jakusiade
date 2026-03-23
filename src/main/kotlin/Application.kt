package pl.wiktorlacki

import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import pl.wiktorlacki.plugins.configureDependencyInjection
import pl.wiktorlacki.plugins.configureRouting
import pl.wiktorlacki.plugins.configureSerialization


fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() = runBlocking {
    configureDependencyInjection()
    configureSerialization()
    configureRouting()
}

