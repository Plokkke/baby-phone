package fr.crntech.babyphone.shared

import kotlin.math.log10
import kotlin.math.sqrt

object Loudness {
    const val FLOOR_DB = -90f

    /** RMS level of 16-bit little-endian PCM, in dBFS (0 = full scale). */
    fun dbfs(pcm: ByteArray): Float {
        val samples = pcm.size / AudioSpec.BYTES_PER_SAMPLE
        if (samples == 0) return FLOOR_DB
        val sumSquares = (0 until samples).sumOf { i ->
            val sample = (pcm[2 * i].toInt() and 0xFF) or (pcm[2 * i + 1].toInt() shl 8)
            sample.toDouble() * sample
        }
        val rms = sqrt(sumSquares / samples) / Short.MAX_VALUE
        return if (rms <= 0.0) FLOOR_DB else maxOf(FLOOR_DB, (20 * log10(rms)).toFloat())
    }
}
