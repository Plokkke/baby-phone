import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm {
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        // Library only: Node is enough to run the tests; browser apps consume the same klib.
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.serialization.json)
            api(libs.serialization.cbor)
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.coroutines.test)
        }
    }
}
