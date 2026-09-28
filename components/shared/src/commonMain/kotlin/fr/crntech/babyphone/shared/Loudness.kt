package fr.crntech.babyphone.shared

import kotlin.math.log10
import kotlin.math.sqrt

object Loudness {
    const val FLOOR_DB = -90f

    /** RMS level of 16-bit little-endian PCM, in dBFS (0 = full scale). */
    fun dbfs(pcm: ByteArray): Float {
        val samples = AudioSpec.samples(pcm)
        if (samples == 0) return FLOOR_DB
        val sumSquares = (0 until samples).sumOf { i -> AudioSpec.sampleAt(pcm, i).toDouble().let { it * it } }
        val rms = sqrt(sumSquares / samples) / Short.MAX_VALUE
        return if (rms <= 0.0) FLOOR_DB else maxOf(FLOOR_DB, (20 * log10(rms)).toFloat())
    }
}
