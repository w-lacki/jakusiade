package pl.wiktorlacki.plugins

import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import pl.wiktorlacki.clients.KoleoClient
import pl.wiktorlacki.services.SeatScoringEngine
import pl.wiktorlacki.services.SeatService

fun Application.configureDependencyInjection() {
    install(Koin) {
        modules(module {
            single { KoleoClient() }
            single { SeatScoringEngine() }
            single { SeatService() }
        })
    }
}
