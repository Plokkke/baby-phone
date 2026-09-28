package fr.crntech.babyphone.net

import android.util.Log
import fr.crntech.babyphone.shared.Peer
import fr.crntech.babyphone.shared.ServerEvent
import fr.crntech.babyphone.shared.Timing
import fr.crntech.babyphone.shared.Wire
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ServerTransport(private val client: HttpClient, private val url: String) : Transport {
    private val _connected = MutableStateFlow(false)
    private val _presence = MutableStateFlow(emptyList<Peer>())
    private val _frames = MutableSharedFlow<ByteArray>(extraBufferCapacity = BUFFER, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val outbox = Channel<ByteArray>(BUFFER, BufferOverflow.DROP_OLDEST)

    override val connected = _connected.asStateFlow()
    override val presence = _presence.asStateFlow()
    override val frames = _frames.asSharedFlow()

    override fun send(frame: ByteArray) {
        if (_connected.value) outbox.trySend(frame)
    }

    override suspend fun run(): Nothing {
        while (true) {
            try {
                connectOnce()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Connection failed: ${e.message}")
            } finally {
                _connected.value = false
                _presence.value = emptyList()
            }
            delay(Timing.RECONNECT_DELAY)
        }
    }

    private suspend fun connectOnce() = client.webSocket(url) {
        while (outbox.tryReceive().isSuccess) Unit
        _connected.value = true
        val writer = launch { for (frame in outbox) send(Frame.Binary(true, frame)) }
        try {
            for (frame in incoming) when (frame) {
                is Frame.Binary -> _frames.emit(frame.readBytes())
                is Frame.Text -> (Wire.decode(frame.readText()) as? ServerEvent.Presence)?.let { _presence.value = it.peers }
                else -> Unit
            }
        } finally {
            writer.cancel()
        }
    }

    private companion object {
        const val TAG = "ServerTransport"
        const val BUFFER = 64
    }
}
