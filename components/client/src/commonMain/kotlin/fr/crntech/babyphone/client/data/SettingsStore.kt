package fr.crntech.babyphone.client.data

import fr.crntech.babyphone.client.platform.KeyValueStorage
import fr.crntech.babyphone.shared.PairingSecret
import fr.crntech.babyphone.shared.Threshold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class AppSettings(
    val deviceId: String,
    val secret: PairingSecret,
    val paired: Boolean,
    /** Trigger margin above the room's background, see [fr.crntech.babyphone.shared.NoiseFloor]. */
    val marginDb: Float,
)

/** Single writer of the device settings; creates the device identity and a pairing secret on first use. */
class SettingsStore(private val storage: KeyValueStorage) {
    private val state = MutableStateFlow(initialize())
    val data = state.asStateFlow()

    fun pairWith(secret: PairingSecret) = write(SECRET to secret.encode(), PAIRED to "true")

    fun markPaired() {
        if (!state.value.paired) write(PAIRED to "true")
    }

    fun resetPairing() = write(*newPairing())

    fun setMargin(db: Float) = write(MARGIN to db.toString())

    @OptIn(ExperimentalUuidApi::class)
    private fun initialize(): AppSettings {
        if (storage.get(DEVICE_ID) == null) storage.put(mapOf(DEVICE_ID to Uuid.random().toString()))
        if (storage.get(SECRET)?.let(PairingSecret::decode) == null) storage.put(mapOf(*newPairing()))
        return read()
    }

    private fun write(vararg values: Pair<String, String>) {
        storage.put(mapOf(*values))
        state.value = read()
    }

    private fun read() = AppSettings(
        deviceId = checkNotNull(storage.get(DEVICE_ID)),
        secret = checkNotNull(storage.get(SECRET)?.let(PairingSecret::decode)),
        paired = storage.get(PAIRED).toBoolean(),
        marginDb = storage.get(MARGIN)?.toFloatOrNull() ?: Threshold.DEFAULT_MARGIN_DB,
    )

    private fun newPairing() = arrayOf(SECRET to PairingSecret.generate().encode(), PAIRED to "false")

    private companion object {
        const val DEVICE_ID = "device_id"
        const val SECRET = "pairing_secret"
        const val PAIRED = "paired"
        const val MARGIN = "threshold_margin_db"
    }
}
