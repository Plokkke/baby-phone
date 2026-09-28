package fr.crntech.babyphone.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import fr.crntech.babyphone.shared.AudioSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

object Microphone {
    /** Cold flow of [AudioSpec.FRAME_BYTES] PCM frames; the recorder lives as long as the collection. */
    @SuppressLint("MissingPermission") // Callers only start sessions once RECORD_AUDIO is granted.
    fun frames(source: Int): Flow<ByteArray> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(AudioSpec.SAMPLE_RATE, CHANNEL, ENCODING)
        val record = AudioRecord(source, AudioSpec.SAMPLE_RATE, CHANNEL, ENCODING, maxOf(minBuffer, AudioSpec.FRAME_BYTES * 10))
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

    private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
    private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
}
