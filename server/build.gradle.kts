plugins {
    application
    kotlin("jvm")
}

dependencies {
    implementation(project(":shared"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.14.0")
    implementation("io.ktor", "ktor-server-netty", "1.2.6")
    implementation("io.ktor", "ktor-websockets", "1.2.6")
    implementation("com.google.flogger:flogger:0.4")
    implementation("com.google.flogger:flogger-system-backend:0.4")
}

application {
    mainClassName = "com.github.jdemeulenaere.game.server.ServerKt"
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}