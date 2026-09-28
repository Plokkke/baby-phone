package fr.crntech.babyphone.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import fr.crntech.babyphone.shared.AudioSpec

/**
 * Streams PCM frames without ever blocking the caller. The track buffer caps latency:
 * when the network bursts, overflowing audio is dropped instead of piling up.
 */
class Speaker(usage: Int = AudioAttributes.USAGE_MEDIA) : AutoCloseable {
    private val track = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(usage).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(AudioSpec.SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
        )
        .setBufferSizeInBytes(AudioSpec.FRAME_BYTES * BUFFERED_FRAMES)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()
        .apply { play() }

    fun play(pcm: ByteArray) {
        track.write(pcm, 0, pcm.size, AudioTrack.WRITE_NON_BLOCKING)
    }

    override fun close() {
        track.stop()
        track.release()
    }

    private companion object {
        const val BUFFERED_FRAMES = 25
    }
}
