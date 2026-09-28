package fr.crntech.babyphone.platform

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import fr.crntech.babyphone.client.platform.MicMode
import fr.crntech.babyphone.client.platform.Microphone
import fr.crntech.babyphone.shared.AudioSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

object AndroidMicrophone : Microphone {
    @SuppressLint("MissingPermission") // Sessions only start once RECORD_AUDIO is granted.
    override fun frames(mode: MicMode): Flow<ByteArray> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(AudioSpec.SAMPLE_RATE, CHANNEL, ENCODING)
        val record = AudioRecord(mode.source, AudioSpec.SAMPLE_RATE, CHANNEL, ENCODING, maxOf(minBuffer, AudioSpec.FRAME_BYTES * 10))
        check(record.state == AudioRecord.STATE_INITIALIZED) { "Microphone unavailable" }
        try {
            record.startRecording()
            while (currentCoroutineContext().isActive) {
                val frame = ByteArray(AudioSpec.FRAME_BYTES)
                val read = record.read(frame, 0, frame.size)
                check(read >= 0) { "Microphone read failed: $read" }
                if (read == frame.size) emit(frame)
            }
        } finally {
            record.stop()
            record.release()
        }
    }.flowOn(Dispatchers.IO)

    private val MicMode.source
        get() = when (this) {
            MicMode.AMBIENT -> MediaRecorder.AudioSource.MIC
            MicMode.VOICE -> MediaRecorder.AudioSource.VOICE_COMMUNICATION
        }

    private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
    private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
}
