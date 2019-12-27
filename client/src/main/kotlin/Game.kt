import PIXI.Application
import PIXI.Graphics
import com.github.jdemeulenaere.game.shared.Constants
import com.github.jdemeulenaere.game.shared.ServerMessage
import com.github.jdemeulenaere.game.shared.State
import kotlin.browser.document

class Game {
    private val application = Application()
    private val renderer = application.renderer
    private val stage = application.stage
    private val players = hashMapOf<Int, Graphics>()

    init {
        document.body!!.appendChild(application.view)
        renderer.view.style.position = "absolute"
        renderer.view.style.display = "block"
        renderer.autoDensity = true
        renderer.backgroundColor = 0x63ccff
        renderer.resize(Constants.MAP_WIDTH, Constants.MAP_HEIGHT)
    }

    fun handleServerMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.SetState -> {
                val state = message.state
                sync(state)
            }
        }
    }

    private fun sync(state: State) {
        val playersToRemove = HashMap(players)
        for (player in state.players) {
            val circle = playersToRemove.remove(player.id)
            val x = player.position.x
            val y = player.position.y

            if (circle != null) {
                // Player already displayed. Update its position.
                circle.x = x
                circle.y = y
            } else {
                // Add the player on the board.
                val newCircle = Graphics()
                newCircle.beginFill(player.color)
                newCircle.drawCircle(0, 0, Constants.PLAYER_RADIUS)
                newCircle.endFill()
                newCircle.x = x
                newCircle.y = y

                stage.addChild(newCircle)
                players[player.id] = newCircle
            }
        }

        // Remove circles from removed players.
        for ((id, circle) in playersToRemove) {
            stage.removeChild(circle)
            players.remove(id)
        }
    }
}