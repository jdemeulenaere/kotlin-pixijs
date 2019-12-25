plugins {
    kotlin("js")
}

dependencies {
    implementation(project(":shared"))
    implementation(kotlin("stdlib-js"))
    implementation("io.ktor", "ktor-client-js", "1.2.6")
    implementation("io.ktor", "ktor-client-websockets-js", "1.2.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:0.14.0")
}

kotlin {
    sourceSets["main"].dependencies {
        implementation(npm("pixi.js", "5.2.0"))
        implementation(npm("text-encoding"))
        implementation(npm("utf-8-validate"))
        implementation(npm("bufferutil"))
        implementation(npm("fs"))
    }

    target {
        browser {}
    }
}