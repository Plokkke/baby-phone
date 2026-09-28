import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

/** Browser device (PC): the shared client running in WebAssembly, served by the server under /web. */
kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "babyphone"
        browser {
            commonWebpackConfig { outputFileName = "babyphone.js" }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":client"))
            implementation(libs.kotlinx.browser)
            implementation(libs.cmp.resources)
            implementation(npm("jsqr", libs.versions.jsqr.get()))
        }
    }
}
