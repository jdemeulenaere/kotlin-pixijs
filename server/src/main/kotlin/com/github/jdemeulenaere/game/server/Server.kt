package com.github.jdemeulenaere.game.server

import com.github.jdemeulenaere.game.shared.ClientMessage
import com.github.jdemeulenaere.game.shared.Constants
import com.github.jdemeulenaere.game.shared.ServerMessage
import com.google.common.flogger.FluentLogger
import io.ktor.application.install
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlin.concurrent.fixedRateTimer

class Server {
    private val log = FluentLogger.forEnclosingClass()

    fun start() {
        val gameHolder = Game.Holder()
        val json = Json(JsonConfiguration.Default)
        val port = 8000
        val server = embeddedServer(Netty, port = port) {
            install(WebSockets)
            routing {
                webSocket("/") {
                    val session = this
                    val messageSender: ServerMessageSender = { message ->
                        try {
                            val text = json.stringify(ServerMessage.serializer(), message)
                            session.send(Frame.Text(text))
                        } catch (e: Exception) {
                            log.atWarning().log("Failed to send frame to %s. Exception: %s", session, e)
                        }
                    }

                    val player = gameHolder.withGame { addPlayer(messageSender) }

                    // Listen for messages from client.
                    for (frame in session.incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val message = try {
                                    json.parse(ClientMessage.serializer(), frame.readText())
                                } catch (e: Exception) {
                                    log.atWarning().log("Failed to parse frame from client: %s", frame)
                                    null
                                }

                                if (message != null) {
                                    gameHolder.withGame { handleClientMessage(player, message) }
                                }
                            }
                            else -> log.atWarning().log("Received unexpected frame: %s", frame)
                        }
                    }

                    gameHolder.withGame { removePlayer(player.id) }
                }
            }
        }

        server.start()
        val tickPeriod = 1_000L / Constants.SERVER_FREQUENCY
        log.atInfo().log("Started server on port %d, ticking every %d ms.", port, tickPeriod)

        // Tick the game at server frequency.
        var lastTick = System.currentTimeMillis()
        fixedRateTimer("gameLoop", period = tickPeriod) {
            val now = System.currentTimeMillis()

            // Tick the game state.
            runBlocking {
                gameHolder.withGame { tick(now - lastTick) }
            }

            lastTick = now
        }
    }
}

fun main() {
    Server().start()
}
