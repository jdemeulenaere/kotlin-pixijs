import PIXI.Graphics
import io.ktor.util.KtorExperimentalAPI
import org.w3c.dom.HTMLCanvasElement
import kotlin.browser.document
import kotlin.browser.window
import PIXI.Application as PixiApplication

class Application {
    val application = PixiApplication()
    val renderer = application.renderer
    val stage = application.stage
    var scale: Double

    init {
        document.body!!.appendChild(application.view)
        renderer.view.style.position = "absolute"
        renderer.view.style.display = "block"
        renderer.autoDensity = true
        renderer.backgroundColor = 0x63ccff
        renderer.resize(1024, 768)
        scale = scaleToWindow(renderer.view)
        window.addEventListener("resize", {
            scale = scaleToWindow(renderer.view)
            println("Scale is now $scale")
        })

        val circle = Graphics()
        circle.beginFill(0x9966FF)
        circle.drawCircle(0, 0, 32)
        circle.endFill()
        circle.x = 64
        circle.y = 130
        stage.addChild(circle)
    }
}

@KtorExperimentalAPI
fun main() {
    println()
}

external fun scaleToWindow(view: HTMLCanvasElement): Double