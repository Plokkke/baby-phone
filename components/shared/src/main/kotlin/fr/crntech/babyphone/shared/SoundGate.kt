package fr.crntech.babyphone.shared

/**
 * Decides which audio frames leave the emitter.
 * Keeps a rolling pre-roll so the start of a cry is not lost,
 * and stays open for a hangover period after the last trigger.
 */
class SoundGate(
    private val preRollFrames: Int = AudioSpec.frames(Timing.PRE_ROLL),
    private val hangoverFrames: Int = AudioSpec.frames(Timing.HANGOVER),
) {
    init {
        require(hangoverFrames > 0) { "Hangover must be at least one frame" }
    }

    private val preRoll = ArrayDeque<ByteArray>(preRollFrames)
    private var hangoverLeft = 0

    val isOpen: Boolean get() = hangoverLeft > 0

    fun process(frame: ByteArray, triggered: Boolean): List<ByteArray> = when {
        triggered -> (preRoll.toList() + frame).also {
            preRoll.clear()
            hangoverLeft = hangoverFrames
        }
        isOpen -> listOf(frame).also { hangoverLeft-- }
        else -> emptyList<ByteArray>().also { remember(frame) }
    }

    private fun remember(frame: ByteArray) {
        if (preRollFrames == 0) return
        if (preRoll.size == preRollFrames) preRoll.removeFirst()
        preRoll.addLast(frame)
    }
}
