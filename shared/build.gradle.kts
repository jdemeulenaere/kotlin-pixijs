plugins {
    kotlin("multiplatform") version "1.3.61"
}

kotlin {
    jvm()
    js {
        browser {}
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }
    }
}