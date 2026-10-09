package fr.crntech.babyphone.shared

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Brings room sound to a listenable level: a voice across the room reaches the microphone at around
 * -55 dBFS, far too quiet to hear once played back. The gain drops at once on a louder frame (never clips)
 * and recovers slowly (no pumping), up to [maxGainDb].
 */
class AutoGain(
    targetPeakDb: Float = -6f,
    maxGainDb: Float = 40f,
    recoveryDbPerSecond: Float = 12f,
) {
    private val targetPeak = gainOf(targetPeakDb) * Short.MAX_VALUE
    private val maxGain = gainOf(maxGainDb)
    private val recoveryPerFrame = gainOf(recoveryDbPerSecond * AudioSpec.FRAME_MS / 1000)
    private var gain = 1f

    fun process(pcm: ByteArray): ByteArray {
        val samples = AudioSpec.samples(pcm)
        val peak = (0 until samples).maxOfOrNull { abs(AudioSpec.sampleAt(pcm, it)) } ?: 0
        val fitting = if (peak == 0) maxGain else min(maxGain, targetPeak / peak)
        gain = min(gain * recoveryPerFrame, fitting)
        return ByteArray(pcm.size).also { out ->
            repeat(samples) { AudioSpec.writeSample(out, it, (AudioSpec.sampleAt(pcm, it) * gain).roundToInt()) }
        }
    }

    private fun gainOf(db: Float) = 10f.pow(db / 20)
}
