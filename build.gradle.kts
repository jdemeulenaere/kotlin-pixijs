plugins {
    id("org.jetbrains.kotlin.js") version "1.3.61"
}

group = "com.github.jdemeulenaere"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-js"))
}

kotlin {
    sourceSets["main"].dependencies {
        implementation(npm("pixi.js", "5.0.0-rc"))
    }
    target.browser {}
}