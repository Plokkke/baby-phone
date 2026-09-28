package fr.crntech.babyphone.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import fr.crntech.babyphone.client.platform.Alarm
import fr.crntech.babyphone.monitor.Notifications

/** Loud, looping alert for a lost link: sound + vibration + high-priority notification. */
class AndroidAlarm(private val context: Context) : Alarm {
    private val ringtone: Ringtone? = RingtoneManager.getRingtone(
        context,
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
    )?.apply {
        audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
        isLooping = true
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    private var ringing = false
    private var notified = false

    override fun raise() {
        if (!notified) Notifications.showAlarm(context).also { notified = true }
        if (ringing) return
        ringing = true
        ringtone?.play()
        vibrator?.vibrate(VibrationEffect.createWaveform(VIBRATION, 0))
    }

    override fun silence() {
        if (!ringing) return
        ringing = false
        ringtone?.stop()
        vibrator?.cancel()
    }

    override fun clear() {
        silence()
        if (notified) Notifications.cancelAlarm(context).also { notified = false }
    }

    private companion object {
        val VIBRATION = longArrayOf(0, 800, 600)
    }
}
