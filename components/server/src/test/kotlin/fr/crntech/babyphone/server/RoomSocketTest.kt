package fr.crntech.babyphone.server

import fr.crntech.babyphone.shared.Endpoints
import fr.crntech.babyphone.shared.PairingSecret
import fr.crntech.babyphone.shared.Peer
import fr.crntech.babyphone.shared.Role
import fr.crntech.babyphone.shared.ServerEvent
import fr.crntech.babyphone.shared.Wire
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoomSocketTest {
    private val room = runBlocking { PairingSecret.generate().roomId() }
    private val config = ServerConfig(version = "1.2.3", androidCertFingerprints = listOf("AA:BB"))

    private fun serverTest(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        application { module(config) }
        block(createClient { install(WebSockets) })
    }

    private suspend fun HttpClient.join(id: String, role: Role, roomId: String = room) =
        webSocketSession(Endpoints.roomSocket("ws://localhost", roomId, Peer(id, id, role)))

    private suspend fun DefaultClientWebSocketSession.nextBinary(): ByteArray? = withTimeoutOrNull(1_000) {
        var frame = incoming.receive()
        while (frame !is Frame.Binary) frame = incoming.receive()
        frame.readBytes()
    }

    private suspend fun DefaultClientWebSocketSession.awaitPeers(count: Int): List<Peer> = withTimeout(1_000) {
        var peers: List<Peer>
        do {
            val frame = incoming.receive()
            peers = (frame as? Frame.Text)?.let { (Wire.decode(it.readText()) as? ServerEvent.Presence)?.peers }.orEmpty()
        } while (peers.size != count)
        peers
    }

    @Test
    fun `emitter frames reach receivers only`() = serverTest { client ->
        val emitter = client.join("baby", Role.EMITTER)
        val receiver = client.join("parent", Role.RECEIVER)
        val idle = client.join("new", Role.IDLE)
        emitter.awaitPeers(3)

        emitter.send(byteArrayOf(1, 2, 3))
        assertContentEquals(byteArrayOf(1, 2, 3), receiver.nextBinary())
        assertNull(idle.nextBinary())
    }

    @Test
    fun `receiver frames reach the emitters and the other receivers, not the sender`() = serverTest { client ->
        val emitter = client.join("baby", Role.EMITTER)
        val receiver = client.join("parent", Role.RECEIVER)
        val otherReceiver = client.join("other-parent", Role.RECEIVER)
        receiver.awaitPeers(3)

        receiver.send(byteArrayOf(9))
        assertContentEquals(byteArrayOf(9), emitter.nextBinary())
        assertContentEquals(byteArrayOf(9), otherReceiver.nextBinary())
        assertNull(receiver.nextBinary())
    }

    @Test
    fun `rooms are isolated`() = serverTest { client ->
        val emitter = client.join("baby", Role.EMITTER)
        val stranger = client.join("x", Role.RECEIVER, PairingSecret.generate().roomId())
        emitter.send(byteArrayOf(1))
        assertNull(stranger.nextBinary())
    }

    @Test
    fun `presence lists connected peers`() = serverTest { client ->
        val emitter = client.join("baby", Role.EMITTER)
        client.join("parent", Role.RECEIVER)
        assertEquals(setOf("baby", "parent"), emitter.awaitPeers(2).map(Peer::deviceId).toSet())
    }

    @Test
    fun `presence carries each device's versions`() = serverTest { client ->
        val receiver = client.join("parent", Role.RECEIVER)
        client.webSocketSession(Endpoints.roomSocket("ws://localhost", room, Peer("baby", "baby", Role.EMITTER, 7, "9.9.9")))
        val baby = receiver.awaitPeers(2).single { it.deviceId == "baby" }
        assertEquals(7 to "9.9.9", baby.protocol to baby.version)
    }

    @Test
    fun `asset links expose the app signature`() = serverTest { client ->
        val body = client.get("/.well-known/assetlinks.json").bodyAsText()
        assertContains(body, "fr.crntech.babyphone")
        assertContains(body, "AA:BB")
    }

    @Test
    fun `health reports the deployed version`() = serverTest { client ->
        assertContains(client.get(Endpoints.HEALTH_PATH).bodyAsText(), "\"version\":\"1.2.3\"")
    }
}
