package fr.crntech.babyphone.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import fr.crntech.babyphone.R
import fr.crntech.babyphone.shared.Role
import fr.crntech.babyphone.ui.MainActivity

object Notifications {
    const val MONITOR_ID = 1
    private const val ALARM_ID = 2
    private const val MONITOR_CHANNEL = "monitor"
    private const val ALARM_CHANNEL = "alarm"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(MONITOR_CHANNEL, context.getString(R.string.notif_channel_monitor), NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(ALARM_CHANNEL, context.getString(R.string.notif_channel_alarm), NotificationManager.IMPORTANCE_HIGH),
            ),
        )
    }

    fun monitoring(context: Context, role: Role): Notification {
        val title = if (role == Role.EMITTER) R.string.notif_emitter else R.string.notif_receiver
        val stop = PendingIntent.getService(
            context, 0, MonitorService.stopIntent(context), PendingIntent.FLAG_IMMUTABLE,
        )
        return base(context, MONITOR_CHANNEL)
            .setContentTitle(context.getString(title))
            .setOngoing(true)
            .addAction(0, context.getString(R.string.notif_stop), stop)
            .build()
    }

    @Suppress("MissingPermission") // Silently skipped when notifications are denied; the in-app alarm still rings.
    fun showAlarm(context: Context) {
        val notification = base(context, ALARM_CHANNEL)
            .setContentTitle(context.getString(R.string.alarm_title))
            .setContentText(context.getString(R.string.alarm_text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(ALARM_ID, notification)
        }
    }

    fun cancelAlarm(context: Context) = NotificationManagerCompat.from(context).cancel(ALARM_ID)

    private fun base(context: Context, channel: String) = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_moon)
        .setContentIntent(
            PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
}
