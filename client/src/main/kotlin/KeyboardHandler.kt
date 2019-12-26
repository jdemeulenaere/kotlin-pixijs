import com.github.jdemeulenaere.game.shared.ClientMessage
import com.github.jdemeulenaere.game.shared.Direction
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

class KeyboardHandler(private val session: WebSocketSession) {
    private val json = Json(JsonConfiguration.Default)
    private val keyboards = setOf(
        keyboard(KeyValues.ARROW_UP, Direction.UP),
        keyboard(KeyValues.ARROW_DOWN, Direction.DOWN),
        keyboard(KeyValues.ARROW_LEFT, Direction.LEFT),
        keyboard(KeyValues.ARROW_RIGHT, Direction.RIGHT)
    )
    private var direction = 0

    private fun keyboard(value: String, direction: Direction): Keyboard {
        return keyboard(value).apply {
            press = { onKeyPress(direction) }
            release = { onKeyRelease(direction) }
        }
    }

    private fun onKeyPress(direction: Direction) {
        this.direction = this.direction or direction.flag
        println("$direction pressed")
        sendMessage()
    }

    private fun onKeyRelease(direction: Direction) {
        this.direction = this.direction and direction.flag.inv()
        println("$direction released")
        sendMessage()
    }

    private fun sendMessage() {
        GlobalScope.launch {
            val json = json.stringify(ClientMessage.serializer(), ClientMessage.SetDirection(direction))
            session.send(Frame.Text(json))
        }
    }

    fun unsubscribe() {
        keyboards.forEach { it.unsubscribe() }
    }
}