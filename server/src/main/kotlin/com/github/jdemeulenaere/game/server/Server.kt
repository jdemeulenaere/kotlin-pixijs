package com.github.jdemeulenaere.game.server

import com.github.jdemeulenaere.game.shared.State
import io.ktor.application.install
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket

fun main() {
    val server = embeddedServer(Netty, port = 8000) {
        install(WebSockets)
        routing {
            webSocket("/") {
                println("New player entered the game :-)")
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> println("Player said: ${frame.readText()}")
                        else -> println("Received unexpected frame: $frame")
                    }
                }
                println("Player left the game :-)")
            }
        }
    }
    server.start(wait = true)
}