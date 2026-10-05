package fr.crntech.babyphone.client.net

import fr.crntech.babyphone.shared.Peer
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Moves opaque frames between the devices of a pairing. Server relay today, LAN later. */
interface Transport {
    val connected: StateFlow<Boolean>
    val presence: StateFlow<List<Peer>>
    val frames: SharedFlow<ByteArray>

    /** Best effort: frames are dropped while disconnected, stale audio is worthless. */
    fun send(frame: ByteArray)

    /** Writes [frame] right away and stops sending: nothing still queued goes out after it. False when disconnected. */
    suspend fun sendLast(frame: ByteArray): Boolean

    /** Keeps the link up, reconnecting forever until cancelled. */
    suspend fun run(): Nothing
}
