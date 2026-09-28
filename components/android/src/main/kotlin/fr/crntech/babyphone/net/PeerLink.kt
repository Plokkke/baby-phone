package fr.crntech.babyphone.net

import fr.crntech.babyphone.shared.PeerCodec
import fr.crntech.babyphone.shared.PeerMessage
import kotlinx.coroutines.flow.mapNotNull

/** Typed, end-to-end encrypted view over a [Transport]. */
class PeerLink(private val transport: Transport, private val codec: PeerCodec) {
    val connected = transport.connected
    val presence = transport.presence
    val messages = transport.frames.mapNotNull(codec::decode)

    fun send(message: PeerMessage) = transport.send(codec.encode(message))

    suspend fun run(): Nothing = transport.run()
}
