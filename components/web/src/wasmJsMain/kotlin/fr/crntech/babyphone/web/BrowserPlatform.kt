@file:OptIn(ExperimentalWasmJsInterop::class)

package fr.crntech.babyphone.web

import fr.crntech.babyphone.client.platform.Alarm
import fr.crntech.babyphone.client.platform.Battery
import fr.crntech.babyphone.client.platform.BatteryState
import fr.crntech.babyphone.client.platform.KeyValueStorage
import fr.crntech.babyphone.client.platform.Platform
import fr.crntech.babyphone.client.platform.QuietMode
import fr.crntech.babyphone.client.platform.QuietState
import fr.crntech.babyphone.client.platform.SoundOutput
import fr.crntech.babyphone.client.platform.SoundOutputState
import fr.crntech.babyphone.client.resources.Res
import fr.crntech.babyphone.client.resources.alarm_text
import fr.crntech.babyphone.client.resources.alarm_title
import fr.crntech.babyphone.shared.Endpoints
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.getString
import org.w3c.notifications.GRANTED
import org.w3c.notifications.Notification
import org.w3c.notifications.NotificationOptions
import org.w3c.notifications.NotificationPermission
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** The web client ships inside the relay, so the relay's version is this client's version. */
suspend fun browserPlatform(): Platform {
    val httpClient = HttpClient(Js) { install(WebSockets) }
    return browserPlatform(httpClient, servedVersion(httpClient))
}

private suspend fun servedVersion(client: HttpClient): String? = runCatching {
    val health = client.get(window.location.origin + Endpoints.HEALTH_PATH).bodyAsText()
    Json.parseToJsonElement(health).jsonObject["version"]?.jsonPrimitive?.content
}.getOrNull()

private fun browserPlatform(httpClient: HttpClient, appVersion: String?) = Platform(
    publicUrl = window.location.origin,
    deviceName = "Navigateur (${window.navigator.platform})",
    appVersion = appVersion,
    httpClient = httpClient,
    storage = LocalStorage,
    microphone = BrowserMicrophone,
    speaker = ::BrowserSpeaker,
    battery = BrowserBattery,
    alarm = ::BrowserAlarm,
    quietMode = BrowserQuietMode,
    soundOutput = BrowserSoundOutput,
)

private object LocalStorage : KeyValueStorage {
    private const val PREFIX = "babyphone."

    override fun get(key: String): String? = localStorage.getItem(PREFIX + key)

    override fun put(values: Map<String, String>) = values.forEach { (key, value) -> localStorage.setItem(PREFIX + key, value) }
}

/** Battery Status API (Chromium only); elsewhere a desktop is assumed to be plugged in. */
private object BrowserBattery : Battery {
    private var state = BatteryState(percent = 100, charging = true)

    init {
        MainScope().launch {
            val battery = getBattery()?.await<BatteryManager>() ?: return@launch
            while (true) {
                state = BatteryState((battery.level * 100).toInt(), battery.charging)
                delay(1.minutes)
            }
        }
    }

    override fun read() = state
}

/** Repeating beeps plus a system notification, the tab being possibly in the background. */
private class BrowserAlarm : Alarm {
    private var beeping: Job? = null
    private var notification: Notification? = null

    override fun raise() {
        if (notification == null && Notification.permission == NotificationPermission.GRANTED) {
            MainScope().launch {
                notification = Notification(getString(Res.string.alarm_title), NotificationOptions(body = getString(Res.string.alarm_text)))
            }
        }
        if (beeping != null) return
        beeping = MainScope().launch {
            while (true) {
                WebAudio.tone(880f, 0.4)
                delay(1.seconds)
            }
        }
    }

    override fun silence() {
        beeping?.cancel()
        beeping = null
    }

    override fun clear() {
        silence()
        notification?.close()
        notification = null
    }
}

/** A web page cannot touch the system do-not-disturb. */
private object BrowserQuietMode : QuietMode {
    override val state = MutableStateFlow(QuietState.UNSUPPORTED)

    override fun setEnabled(enabled: Boolean) = Unit
}

/** The system volume is invisible to pages: only browser-side blockers can be detected. */
private object BrowserSoundOutput : SoundOutput {
    private val _state = MutableStateFlow(read())
    override val state = _state.asStateFlow()

    init {
        MainScope().launch {
            while (true) {
                _state.value = read()
                delay(2.seconds)
            }
        }
    }

    /** Must run in a click handler: that is when browsers allow audio and permission prompts. */
    override fun makeAudible() {
        WebAudio.unlock()
        Notification.requestPermission()
    }

    override fun playTestSound() {
        WebAudio.unlock()
        WebAudio.tone(660f, 0.15)
        WebAudio.tone(880f, 0.25, delayS = 0.18)
    }

    private fun read() = SoundOutputState(
        muted = WebAudio.context.state != "running",
        alertsBlocked = Notification.permission != NotificationPermission.GRANTED,
    )
}
