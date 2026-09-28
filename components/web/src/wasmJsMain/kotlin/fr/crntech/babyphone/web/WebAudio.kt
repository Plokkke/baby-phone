@file:OptIn(ExperimentalWasmJsInterop::class)

package fr.crntech.babyphone.web

import fr.crntech.babyphone.client.platform.MicMode
import fr.crntech.babyphone.client.platform.Microphone
import fr.crntech.babyphone.client.platform.Speaker
import fr.crntech.babyphone.shared.AudioSpec
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.MessageEvent
import org.w3c.dom.mediacapture.MediaStream
import org.w3c.dom.mediacapture.MediaStreamConstraints

/** One AudioContext for the whole page; browsers only start it after a user gesture, see [unlock]. */
object WebAudio {
    val context by lazy { AudioContext() }
    private var workletLoaded = false

    /** Call from a click handler so autoplay policies let audio start. */
    fun unlock() {
        if (context.state != "running") context.resume()
    }

    suspend fun ensureCaptureWorklet() {
        if (workletLoaded) return
        context.audioWorklet.addModule("pcm-capture.js").await<JsAny?>()
        workletLoaded = true
    }
}

object BrowserMicrophone : Microphone {
    override fun frames(mode: MicMode): Flow<ByteArray> = callbackFlow {
        val context = WebAudio.context
        val constraints = microphoneConstraints(processed = mode == MicMode.VOICE).unsafeCast<MediaStreamConstraints>()
        val stream = window.navigator.mediaDevices.getUserMedia(constraints).await<MediaStream>()
        WebAudio.ensureCaptureWorklet()

        val source = context.createMediaStreamSource(stream)
        val antiAliasing = context.createBiquadFilter().apply {
            type = "lowpass"
            frequency.value = ANTI_ALIASING_HZ
        }
        val capture = AudioWorkletNode(context, "pcm-capture")
        // The worklet only runs while pulled by the destination; a muted gain keeps it silent.
        val sink = context.createGain().apply { gain.value = 0f }
        capture.port.onmessage = { event: MessageEvent -> trySend(event.data!!.unsafeCast<ArrayBuffer>().toByteArray()) }
        source.connect(antiAliasing).connect(capture).connect(sink).connect(context.destination)

        awaitClose {
            capture.port.onmessage = null
            listOf(source, antiAliasing, capture, sink).forEach(AudioNode::disconnect)
            stream.getTracks().toList().forEach { it.stop() }
        }
    }

    private fun ArrayBuffer.toByteArray(): ByteArray = Int8Array(this).let { bytes -> ByteArray(bytes.length) { bytes[it] } }

    private const val ANTI_ALIASING_HZ = 7_000f
}

/** Schedules each frame right after the previous one; frames too far ahead are dropped to cap latency. */
class BrowserSpeaker : Speaker {
    private val context = WebAudio.context
    private var playhead = 0.0

    override fun play(pcm: ByteArray) {
        val samples = AudioSpec.samples(pcm)
        val buffer = context.createBuffer(1, samples, AudioSpec.SAMPLE_RATE.toFloat())
        val channel = buffer.getChannelData(0)
        repeat(samples) { channel[it] = AudioSpec.sampleAt(pcm, it) / FULL_SCALE }

        val now = context.currentTime
        if (playhead < now) playhead = now + JITTER_BUFFER_S
        if (playhead - now > MAX_LATENCY_S) return
        context.createBufferSource().apply {
            this.buffer = buffer
            connect(context.destination)
            start(playhead)
        }
        playhead += samples.toDouble() / AudioSpec.SAMPLE_RATE
    }

    override fun close() = Unit

    private companion object {
        const val FULL_SCALE = 32_768f
        const val JITTER_BUFFER_S = 0.08
        const val MAX_LATENCY_S = 0.5
    }
}
