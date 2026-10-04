package fr.crntech.babyphone.client.net

import fr.crntech.babyphone.shared.Envelope
import fr.crntech.babyphone.shared.PeerCodec
import fr.crntech.babyphone.shared.PeerMessage
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull

/** Typed, end-to-end encrypted view over a [Transport], seen from the device [self]. */
class PeerLink(private val transport: Transport, private val codec: PeerCodec, val self: String) {
    val connected = transport.connected
    val presence = transport.presence

    /** Messages for this device: broadcasts and those addressed to it. */
    val messages = transport.frames.mapNotNull { codec.decode(it) }.filter { it.to == null || it.to == self }

    suspend fun send(message: PeerMessage, to: String? = null) = transport.send(codec.encode(Envelope(self, to, message)))

    suspend fun run(): Nothing = transport.run()
}
