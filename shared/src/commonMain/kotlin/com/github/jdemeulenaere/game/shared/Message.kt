package com.github.jdemeulenaere.game.shared

import kotlinx.serialization.Serializable

@Serializable
sealed class ServerMessage {
    @Serializable
    class SetState(val state: State) : ServerMessage()
}

@Serializable
sealed class ClientMessage {
    @Serializable
    class SetDirection(val direction: Int) : ClientMessage()
}

@Serializable
data class State(var players: List<Player>)

@Serializable
data class Player(
    val id: Int,
    val color: Int,
    var direction: Int,
    val position: Coordinates<Int>,
    val speed: Coordinates<Double>,
    val acceleration: Coordinates<Double>
)

@Serializable
data class Coordinates<T : Number>(
    var x: T,
    var y: T
)

enum class Direction(val flag: Int, val dx: Int = 0, val dy: Int = 0) {
    UP(1, dy = -1),
    DOWN(1 shl 1, dy = +1),
    LEFT(1 shl 2, dx = -1),
    RIGHT(1 shl 3, dx = +1)
}