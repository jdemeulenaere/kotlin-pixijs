import PIXI.Graphics
import com.github.jdemeulenaere.game.shared.ServerMessage
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.webSocketSession
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlin.browser.document
import PIXI.Application
import com.github.jdemeulenaere.game.shared.Constants
import com.github.jdemeulenaere.game.shared.State

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

    fun sync(state: State) {
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
                println("Adding circle")
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
            println("Removing circle")
            stage.removeChild(circle)
            players.remove(id)
        }
    }
}

suspend fun main() {
    val game = Game()
    val client = HttpClient {
        install(WebSockets)
    }

    val json = Json(JsonConfiguration.Default)
    val session = client.webSocketSession(port = 8000)
    val keyboardHandler = KeyboardHandler(session)

    for (frame in session.incoming) {
        when (frame) {
            is Frame.Text -> {
                val message = try {
                    try {
                        val text = frame.readText()
                        json.parse(ServerMessage.serializer(), text)
                    } catch (e: Exception) {
                        println("Failed to read text from server: $e")
                        null
                    }
                } catch (e: Exception) {
                    println("Failed to parse frame from server: $e")
                    null
                }

                when (message) {
                    is ServerMessage.SetState -> {
                        val state = message.state
                        game.sync(state)
                    }
                }
            }
            else -> println("Received unsupported frame from server: $frame")
        }
    }

    keyboardHandler.unsubscribe()
    session.close()
}
