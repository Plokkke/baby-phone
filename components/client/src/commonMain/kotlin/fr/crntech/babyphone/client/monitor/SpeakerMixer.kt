package fr.crntech.babyphone.client.monitor

import fr.crntech.babyphone.client.platform.Speaker

/** One speaker per sender, so two voices are mixed instead of their frames being interleaved. */
class SpeakerMixer(private val create: () -> Speaker) : AutoCloseable {
    private val speakers = HashMap<String, Speaker>()

    fun play(from: String, pcm: ByteArray) = speakers.getOrPut(from, create).play(pcm)

    override fun close() = speakers.values.forEach(Speaker::close)
}
