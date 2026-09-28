package fr.crntech.babyphone.server

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlin.time.Duration.Companion.seconds

fun main() {
    val config = ServerConfig.fromEnv()
    embeddedServer(Netty, port = config.port) { module(config) }.start(wait = true)
}

fun Application.module(config: ServerConfig = ServerConfig.fromEnv()) {
    install(CallLogging)
    install(Compression)
    install(CachingHeaders)
    install(WebSockets) {
        pingPeriod = 5.seconds
        timeout = 15.seconds
        maxFrameSize = MAX_FRAME_BYTES
    }
    val registry = RoomRegistry(config.maxMembersPerRoom)
    routing {
        webRoutes(config)
        roomSocket(registry)
    }
}

private const val MAX_FRAME_BYTES = 64L * 1024
