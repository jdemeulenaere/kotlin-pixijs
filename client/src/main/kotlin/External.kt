import org.w3c.dom.HTMLCanvasElement

external fun keyboard(keyValue: String): Keyboard

interface Keyboard {
    var press: (() -> Unit)?
    var release: (() -> Unit)?
    fun unsubscribe()
}

object KeyValues {
    const val ARROW_UP = "ArrowUp"
    const val ARROW_DOWN = "ArrowDown"
    const val ARROW_LEFT = "ArrowLeft"
    const val ARROW_RIGHT = "ArrowRight"
}