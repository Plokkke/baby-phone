package fr.crntech.babyphone.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.crntech.babyphone.shared.PairingSecret
import fr.crntech.babyphone.shared.Threshold
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import java.util.UUID

data class AppSettings(
    val deviceId: String,
    val secret: PairingSecret,
    val paired: Boolean,
    val thresholdDb: Float,
)

private val Context.dataStore by preferencesDataStore("settings")

class SettingsStore(context: Context) {
    private val store = context.dataStore

    /** Emits once [initialize] has created the device identity and a pairing secret. */
    val data: Flow<AppSettings> = store.data.mapNotNull { it.toSettings() }

    suspend fun current() = data.first()

    suspend fun initialize() = store.edit { prefs ->
        if (prefs[DEVICE_ID] == null) prefs[DEVICE_ID] = UUID.randomUUID().toString()
        if (prefs[SECRET]?.let(PairingSecret::decode) == null) prefs.startNewPairing()
    }

    suspend fun pairWith(secret: PairingSecret) = store.edit {
        it[SECRET] = secret.encode()
        it[PAIRED] = true
    }

    suspend fun markPaired() = store.edit { it[PAIRED] = true }

    suspend fun resetPairing() = store.edit { it.startNewPairing() }

    suspend fun setThreshold(db: Float) = store.edit { it[THRESHOLD] = db }

    private fun MutablePreferences.startNewPairing() {
        this[SECRET] = PairingSecret.generate().encode()
        this[PAIRED] = false
    }

    private fun Preferences.toSettings(): AppSettings? {
        val deviceId = this[DEVICE_ID] ?: return null
        val secret = this[SECRET]?.let(PairingSecret::decode) ?: return null
        return AppSettings(deviceId, secret, this[PAIRED] ?: false, this[THRESHOLD] ?: Threshold.DEFAULT_DB)
    }

    private companion object {
        val DEVICE_ID = stringPreferencesKey("device_id")
        val SECRET = stringPreferencesKey("pairing_secret")
        val PAIRED = booleanPreferencesKey("paired")
        val THRESHOLD = floatPreferencesKey("threshold_db")
    }
}
