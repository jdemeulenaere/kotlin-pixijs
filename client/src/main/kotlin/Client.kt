import com.github.jdemeulenaere.game.shared.ServerMessage
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.webSocketSession
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

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
                    json.parse(ServerMessage.serializer(), frame.readText())
                } catch (e: Exception) {
                    println("Failed to parse frame from server: $e")
                    null
                }

                message?.let { game.handleServerMessage(it) }
            }
            else -> println("Received unsupported frame from server: $frame")
        }
    }

    keyboardHandler.unsubscribe()
    session.close()
}
