plugins {
    id("org.jetbrains.kotlin.js") version "1.3.61"
}

dependencies {
    implementation(project(":shared"))
    implementation(kotlin("stdlib-js"))
    implementation("io.ktor", "ktor-client-js", "1.2.6")
    implementation("io.ktor", "ktor-client-websockets", "1.2.6")
}

kotlin {
    sourceSets["main"].dependencies {
        implementation(npm("pixi.js", "5.2.0"))
    }

    target {
        browser {}
        useCommonJs()
    }
}