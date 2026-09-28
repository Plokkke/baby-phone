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
    implementation(libs.logback)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.websockets)
}

tasks.test { useJUnitPlatform() }
