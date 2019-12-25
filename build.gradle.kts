plugins {
    kotlin("multiplatform") version "1.3.61" apply false
    kotlin("plugin.serialization") version "1.3.61" apply false
}

subprojects {
    group = "com.github.jdemeulenaere"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        jcenter()
    }
}
