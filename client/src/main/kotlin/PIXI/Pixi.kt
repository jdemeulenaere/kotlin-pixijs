@file:Suppress("PackageName")
@file:JsModule("pixi.js")
@file:JsNonModule
package PIXI

import org.w3c.dom.HTMLCanvasElement

external class Application {
    var view: HTMLCanvasElement
    var renderer: Renderer
    var stage: Container
}

external class Renderer {
    var view: HTMLCanvasElement
    var autoDensity: Boolean
    var backgroundColor: Number

    fun resize(screenWidth: Number, screenHeight: Number)
}

open external class DisplayObject {
    var x: Number
    var y: Number
}

open external class Container : DisplayObject {
    fun addChild(vararg child: DisplayObject)
    fun removeChild(child: DisplayObject)
}

open external class Graphics : Container {
    open fun beginFill(color: Number): Graphics
    open fun drawCircle(x: Number, y: Number, radius: Number): Graphics
    open fun endFill(): Graphics
}
