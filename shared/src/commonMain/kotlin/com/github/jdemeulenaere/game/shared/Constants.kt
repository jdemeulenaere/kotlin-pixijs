package com.github.jdemeulenaere.game.shared

object Constants {
    const val MAP_WIDTH = 1024
    const val MAP_HEIGHT = 768
    const val PLAYER_RADIUS = 32
    const val MAX_SPEED = 150.0 // in pixels per second.
    const val ACCELERATION = MAX_SPEED / 3.0 // it takes 3 seconds to get to max speed.
    const val SERVER_FREQUENCY = 1 // Tick x times per second.
}
