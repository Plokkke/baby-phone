plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    application
}

kotlin { jvmToolchain(21) }

application { mainClass = "fr.crntech.babyphone.server.ApplicationKt" }

ktor { fatJar { archiveFileName = "babyphone-server.jar" } }

dependencies {
    implementation(project(":shared"))
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.caching.headers)
    implementation(libs.logback)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.websockets)
}

tasks.test { useJUnitPlatform() }

// Bundle the browser client when it is part of the build (it needs the Android SDK, see -PskipAndroid).
if (findProject(":web") != null) {
    tasks.processResources {
        dependsOn(":web:wasmJsBrowserDistribution")
        from(rootProject.layout.projectDirectory.dir("components/web/build/dist/wasmJs/productionExecutable")) {
            into("web")
            exclude("*.map")
        }
    }
}
