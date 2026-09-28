package fr.crntech.babyphone.platform

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import fr.crntech.babyphone.client.platform.Speaker
import fr.crntech.babyphone.shared.AudioSpec

/** The track buffer caps latency: when the network bursts, overflowing audio is dropped. */
class AndroidSpeaker(usage: Int = AudioAttributes.USAGE_MEDIA) : Speaker {
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

    override fun play(pcm: ByteArray) {
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
