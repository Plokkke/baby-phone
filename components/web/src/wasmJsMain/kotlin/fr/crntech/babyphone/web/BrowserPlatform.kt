@file:OptIn(ExperimentalWasmJsInterop::class)

package fr.crntech.babyphone.web

import fr.crntech.babyphone.client.platform.Alarm
import fr.crntech.babyphone.client.platform.Battery
import fr.crntech.babyphone.client.platform.BatteryState
import fr.crntech.babyphone.client.platform.KeyValueStorage
import fr.crntech.babyphone.client.platform.Platform
import fr.crntech.babyphone.client.resources.Res
import fr.crntech.babyphone.client.resources.alarm_text
import fr.crntech.babyphone.client.resources.alarm_title
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.w3c.notifications.GRANTED
import org.w3c.notifications.Notification
import org.w3c.notifications.NotificationOptions
import org.w3c.notifications.NotificationPermission
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun browserPlatform() = Platform(
    publicUrl = window.location.origin,
    deviceName = "Navigateur (${window.navigator.platform})",
    httpClient = HttpClient(Js) { install(WebSockets) },
    storage = LocalStorage,
    microphone = BrowserMicrophone,
    speaker = ::BrowserSpeaker,
    battery = BrowserBattery,
    alarm = ::BrowserAlarm,
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
                beep()
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

    private fun beep() {
        val context = WebAudio.context
        val now = context.currentTime
        val tone = context.createOscillator().apply {
            type = "square"
            frequency.value = 880f
        }
        val volume = context.createGain().apply { gain.value = 0.2f }
        tone.connect(volume).connect(context.destination)
        tone.start(now)
        tone.stop(now + 0.4)
    }
}
