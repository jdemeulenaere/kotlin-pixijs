package com.github.jdemeulenaere.game.server

import com.github.jdemeulenaere.game.shared.*
import io.ktor.application.install
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlin.random.Random

// Not thread-safe.
class Game {
    var state = State(players = arrayListOf())
    val sessionToPlayer = hashMapOf<WebSocketSession, Player>()
    private var currentId = 0
    private val random = Random(System.currentTimeMillis())

    fun addPlayer(session: WebSocketSession): Player {
        // Add a player with random position.
        val x = random.nextInt(
            from = Constants.PLAYER_RADIUS,
            until = Constants.MAP_WIDTH - Constants.PLAYER_RADIUS + 1
        )

        val y = random.nextInt(
            from = Constants.PLAYER_RADIUS,
            until = Constants.MAP_HEIGHT - Constants.PLAYER_RADIUS + 1
        )

        val player = Player(
            id = currentId++,
            color = random.nextInt(256),
            position = Coordinates(x, y),
            speed = Coordinates(0, 0)
        )

        val previous = sessionToPlayer.put(session, player)
        state.players.add(player)
        if (previous != null) {
            println("Weird, player $previous was already associated to $session")
            state.players.remove(previous)
        }
        return player
    }

    fun removePlayer(session: WebSocketSession) {
        val previous = sessionToPlayer.remove(session)
        if (previous != null) {
            state.players.remove(previous)
        } else {
            println("Weird, no player was associated to $session")
        }
    }

    fun tick(deltaMs: Long) {

    }
}

fun main() {
    val game = Game()
    val json = Json(JsonConfiguration.Default)

    val server = embeddedServer(Netty, port = 8000) {
        install(WebSockets)
        routing {
            webSocket("/") {
                // Player enters game.
                val player = synchronized(game) {
                    game.addPlayer(this)
                }
                println("Player ${player.id} entered the game.")

                // Messages from player.
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> println("Player said: ${frame.readText()}")
                        else -> println("Received unexpected frame: $frame")
                    }
                }

                // Player left the game.
                println("Player ${player.id} left the game!")
                synchronized(game) {
                    game.removePlayer(this)
                }
            }
        }
    }
    server.start()

    // Tick the game at server frequency.
    val start = System.currentTimeMillis()
    var lastTick = start
    var next = start
    var loop = 1
    fun updateNext() {
        next = start + loop * (1_000 / Constants.SERVER_FREQUENCY)
    }

    while (true) {
        loop++
        updateNext()
        while (System.currentTimeMillis() < next) {}
        val current = System.currentTimeMillis()

        // Tick the game state.
        runBlocking {
            val (state, sessions) = synchronized(game) {
                game.tick(current - lastTick)
                game.state to game.sessionToPlayer.keys
            }

            // Broadcast the game state to all clients.
            val msg = ServerMessage.SetState(state)
            val text = json.stringify(ServerMessage.serializer(), msg)
            val frame = Frame.Text(text)
            println("Broadcasting $text")
            sessions.forEach { session ->
                try {
                    session.send(frame)
                } catch (e: Exception) {}
            }
        }

        lastTick = current
    }
}