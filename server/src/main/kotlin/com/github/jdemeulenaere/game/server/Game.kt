package com.github.jdemeulenaere.game.server

import com.github.jdemeulenaere.game.shared.ClientMessage
import com.github.jdemeulenaere.game.shared.Constants
import com.github.jdemeulenaere.game.shared.Coordinates
import com.github.jdemeulenaere.game.shared.Direction
import com.github.jdemeulenaere.game.shared.Player
import com.github.jdemeulenaere.game.shared.ServerMessage
import com.github.jdemeulenaere.game.shared.State
import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

typealias ServerMessageSender = suspend (ServerMessage) -> Unit

private class ServerPlayer(
    val player: Player,
    val messageSender: ServerMessageSender
)

// Not thread-safe.
class Game private constructor() {
    companion object {
        private const val MIN_X = Constants.PLAYER_RADIUS
        private const val MIN_Y = Constants.PLAYER_RADIUS
        private const val MAX_X = Constants.MAP_WIDTH - Constants.PLAYER_RADIUS
        private const val MAX_Y = Constants.MAP_HEIGHT - Constants.PLAYER_RADIUS
    }

    // Utility class that ensures that all calls to Game are executed in the same thread.
    class Holder() {
        private val game = Game()
        private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

        suspend fun <T> withGame(f: suspend Game.() -> T): T {
            return withContext(dispatcher) {
                game.f()
            }
        }
    }

    private val log = FluentLogger.forEnclosingClass()
    private val players = hashMapOf<Int, ServerPlayer>()
    private val random = Random(System.currentTimeMillis())
    private val state = State(players = emptyList())
    private var currentId = 0

    fun addPlayer(messageSender: ServerMessageSender): Player {
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

        val serverPlayer = ServerPlayer(player, messageSender)
        players[player.id] = serverPlayer
        updateStatePlayers()
        log.atInfo().log("Player %d entered the game.", player.id)

        return player
    }

    fun removePlayer(id: Int) {
        val player = players.remove(id)
        updateStatePlayers()
        log.atInfo().log("Player %d left the game!", id)

        if (player == null) {
            log.atWarning().log("Weird, no player was associated to ID %d", id)
        }
    }

    private fun updateStatePlayers() {
        state.players = players.values.map { it.player }.toList()
    }

    fun handleClientMessage(player: Player, message: ClientMessage) {
        when (message) {
            is ClientMessage.SetDirection -> player.direction = message.direction
        }
    }

    suspend fun tick(deltaMs: Long) {
        // Compute new state.
        for (serverPlayer in players.values) {
            val player = serverPlayer.player

            // Compute horizontal and vertical acceleration.
            var ax = 0.0
            var ay = 0.0
            for (direction in Direction.values()) {
                if (player.direction and direction.flag != 0) {
                    ax += direction.dx * Constants.ACCELERATION
                    ay += direction.dy * Constants.ACCELERATION
                }
            }

            // Speed and position.
            player.speed.x = computeSpeed(player.speed.x, ax, deltaMs)
            player.speed.y = computeSpeed(player.speed.y, ay, deltaMs)
            player.position.x = (player.position.x + player.speed.x)
                .roundToInt()
                .coerceIn(MIN_X, MAX_X)
            player.position.y = (player.position.y + player.speed.y)
                .roundToInt()
                .coerceIn(MIN_Y, MAX_Y)
        }

        // Broadcast.
        val message = ServerMessage.SetState(state)
        players.values.forEach { serverPlayer ->
            serverPlayer.messageSender(message)
        }
    }

    private fun computeSpeed(currentSpeed: Double, acceleration: Double, deltaMs: Long): Double {
        return when {
            acceleration == 0.0 -> 0.0
            acceleration > 0.0 -> max(0.0, currentSpeed) + acceleration * deltaMs / 1_000
            else -> min(0.0, currentSpeed) + acceleration * deltaMs / 1_000
        }.coerceIn(-Constants.MAX_SPEED, Constants.MAX_SPEED)
    }
}
