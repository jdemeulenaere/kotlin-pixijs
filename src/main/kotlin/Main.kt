import PIXI.Graphics
import PIXI.Application as PixiApplication
import org.w3c.dom.HTMLCanvasElement
import kotlin.browser.document
import kotlin.browser.window

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

fun main() {
    Application()
}

external fun scaleToWindow(view: HTMLCanvasElement): Double