import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

/** Platform-independent client: sessions, transport, settings and the Compose UI. */
kotlin {
    android {
        namespace = "fr.crntech.babyphone.client"
        compileSdk = 37
        minSdk = 29
        androidResources { enable = true }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            api(libs.coroutines.core)
            api(libs.ktor.client.core)
            api(libs.ktor.client.websockets)
            api(libs.cmp.runtime)
            api(libs.cmp.foundation)
            api(libs.cmp.ui)
            api(libs.cmp.material3)
            api(libs.jb.lifecycle.viewmodel.compose)
            implementation(libs.jb.lifecycle.runtime.compose)
            implementation(libs.cmp.resources)
            implementation(libs.cmp.icons.extended)
            implementation(libs.qrose)
        }
    }
}

compose.resources {
    packageOfResClass = "fr.crntech.babyphone.client.resources"
    publicResClass = true
}
