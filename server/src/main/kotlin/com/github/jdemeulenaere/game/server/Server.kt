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
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlin.random.Random

// Not thread-safe.
class Game private constructor() {
    companion object {
        private const val MIN_X = Constants.PLAYER_RADIUS
        private const val MIN_Y = Constants.PLAYER_RADIUS
        private const val MAX_X = Constants.MAP_WIDTH - Constants.PLAYER_RADIUS
        private const val MAX_Y = Constants.MAP_HEIGHT - Constants.PLAYER_RADIUS
    }

    // Utility class that ensures that all calls to Game are executed in the same thread.
    class Holder {
        private val game = Game()
        private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

        suspend fun withGame(f: suspend Game.() -> Unit) {
            withContext(dispatcher) {
                game.f()
            }
        }
    }

    private val sessionToPlayer = hashMapOf<WebSocketSession, Player>()
    private val json = Json(JsonConfiguration.Default)
    private val random = Random(System.currentTimeMillis())
    private val state = State(players = emptyList())
    private var currentId = 0

    suspend fun handleSession(session: WebSocketSession) {
        val player = addPlayer(session)
        println("Player ${player.id} entered the game.")
        listenPlayer(session, player)
        println("Player ${player.id} left the game!")
        removePlayer(session)
    }

    private fun addPlayer(session: WebSocketSession): Player {
        // Add a player with random position.
        val x = random.nextInt(from = MIN_X, until = MAX_X + 1)
        val y = random.nextInt(from = MIN_Y, until = MAX_Y + 1)

        val color = random.nextInt(256)
        val player = Player(
            id = currentId++,
            color = color,
            direction = 0,
            position = Coordinates(x, y),
            speed = Coordinates(0.0, 0.0),
            acceleration = Coordinates(0.0, 0.0)
        )

        val previous = sessionToPlayer.put(session, player)
        state.players = sessionToPlayer.values.toList()
        if (previous != null) {
            println("Weird, player $previous was already associated to $session")
        }

        return player
    }

    private fun removePlayer(session: WebSocketSession) {
        val previous = sessionToPlayer.remove(session)
        state.players = sessionToPlayer.values.toList()

        if (previous == null) {
            println("Weird, no player was associated to $session")
        }
    }

    private suspend fun listenPlayer(session: WebSocketSession, player: Player) {
        // Messages from player.
        for (frame in session.incoming) {
            when (frame) {
                is Frame.Text -> {
                    val message = try {
                        json.parse(ClientMessage.serializer(), frame.readText())
                    } catch (e: Exception) {
                        println("Failed to parse frame from client: $frame")
                        null
                    }

                    message?.let { handleClientMessage(player, message) }
                }
                else -> println("Received unexpected frame: $frame")
            }
        }
    }

    private fun handleClientMessage(player: Player, message: ClientMessage) {
        when (message) {
            is ClientMessage.SetDirection -> player.direction = message.direction
        }
    }

    suspend fun tick(deltaMs: Long) {
        // Compute new state.
        for (player in sessionToPlayer.values) {
            // Compute horizontal and vertical acceleration.
            var ax = 0.0
            var ay = 0.0
            for (direction in Direction.values()) {
                if (player.direction and direction.flag != 0) {
                    ax += direction.dx * Constants.ACCELERATION
                    ay += direction.dy * Constants.ACCELERATION
                }
            }

            // Speed.
            if (ax == 0.0) {
                player.speed.x = 0.0
            } else {
                player.speed.x = (player.speed.x + ax * deltaMs / 1_000)
                    .coerceIn(-Constants.MAX_SPEED, Constants.MAX_SPEED)
            }

            if (ay == 0.0) {
                player.speed.y = 0.0
            } else {
                player.speed.y = (player.speed.y + ay * deltaMs / 1_000)
                    .coerceIn(-Constants.MAX_SPEED, Constants.MAX_SPEED)
            }

            // Position.
            player.position.x = (player.position.x + player.speed.x)
                .roundToInt()
                .coerceIn(MIN_X, MAX_X)
            player.position.y = (player.position.y + player.speed.y)
                .roundToInt()
                .coerceIn(MIN_Y, MAX_Y)
        }

        // Broadcast.
        val msg = ServerMessage.SetState(state)
        val text = json.stringify(ServerMessage.serializer(), msg)
        sessionToPlayer.keys.forEach { session ->
            try {
                session.send(Frame.Text(text))
            } catch (e: Exception) {
                println("Failed to send frame to $session. Exception: $e")
            }
        }
    }
}

fun main() {
    val gameHolder = Game.Holder()

    val server = embeddedServer(Netty, port = 8000) {
        install(WebSockets)
        routing {
            webSocket("/") {
                val session = this
                gameHolder.withGame { handleSession(session) }
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
        while (System.currentTimeMillis() < next) {
        }
        val current = System.currentTimeMillis()

        // Tick the game state.
        runBlocking {
            gameHolder.withGame { tick(current - lastTick) }
        }

        lastTick = current
    }
}