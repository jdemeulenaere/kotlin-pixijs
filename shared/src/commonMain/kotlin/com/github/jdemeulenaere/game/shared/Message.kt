package com.github.jdemeulenaere.game.shared

import kotlinx.serialization.Serializable

@Serializable
sealed class ServerMessage {
    @Serializable
    class SetState(val state: State) : ServerMessage()
}

@Serializable
data class State(val players: MutableList<Player>)

@Serializable
data class Player(
    val id: Int,
    val color: Int,
    val position: Coordinates,
    val speed: Coordinates
)

@Serializable
data class Coordinates(
    val x: Int,
    val y: Int
)