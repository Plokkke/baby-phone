package fr.crntech.babyphone.platform

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import fr.crntech.babyphone.BuildConfig
import fr.crntech.babyphone.client.platform.KeyValueStorage
import fr.crntech.babyphone.client.platform.Platform
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import kotlin.time.Duration.Companion.seconds

fun androidPlatform(context: Context): Platform {
    val quietMode = AndroidQuietMode(context)
    return Platform(
        publicUrl = BuildConfig.PUBLIC_URL,
        deviceName = Build.MODEL,
        httpClient = HttpClient(OkHttp) {
            install(WebSockets) { pingInterval = 5.seconds }
        },
        storage = PreferencesStorage(context),
        microphone = AndroidMicrophone,
        speaker = { AndroidSpeaker() },
        battery = AndroidBattery(context),
        alarm = { AndroidAlarm(context) },
        quietMode = quietMode,
        soundOutput = AndroidSoundOutput(context, quietMode),
    )
}

private class PreferencesStorage(context: Context) : KeyValueStorage {
    private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    override fun get(key: String): String? = preferences.getString(key, null)

    override fun put(values: Map<String, String>) = preferences.edit(commit = true) {
        values.forEach { (key, value) -> putString(key, value) }
    }
}
