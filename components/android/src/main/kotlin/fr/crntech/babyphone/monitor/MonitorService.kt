package fr.crntech.babyphone.monitor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import fr.crntech.babyphone.R
import fr.crntech.babyphone.container
import fr.crntech.babyphone.device.Battery
import fr.crntech.babyphone.shared.Role
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.hours

/** Keeps the monitoring session alive with the screen off. */
class MonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val wakeLock by lazy {
        getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "babyphone:monitor")
    }
    private val wifiLock by lazy {
        getSystemService(WifiManager::class.java).createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "babyphone:monitor")
    }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val role = intent?.getStringExtra(EXTRA_ROLE)?.let(Role::valueOf)
        if (role == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(this, Notifications.MONITOR_ID, Notifications.monitoring(this, role), foregroundTypes(role))
        acquireLocks()
        job?.cancel()
        job = scope.launch { runSession(role) }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        MonitorHub.publish(null)
        if (wakeLock.isHeld) wakeLock.release()
        if (wifiLock.isHeld) wifiLock.release()
        super.onDestroy()
    }

    private suspend fun runSession(role: Role) {
        val session = createSession(role)
        MonitorHub.publish(session)
        try {
            session.run()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Session failed", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MonitorService, R.string.monitor_failed, Toast.LENGTH_LONG).show()
                stopSelf()
            }
        }
    }

    private suspend fun createSession(role: Role): MonitorSession {
        val settings = container.settings.current()
        val link = container.peerLink(settings.secret, settings.deviceId, role)
        return when (role) {
            Role.EMITTER -> EmitterSession(link, container.settings, Battery(this), settings.thresholdDb)
            Role.RECEIVER -> ReceiverSession(link, Alarm(this))
            Role.IDLE -> error("Idle devices do not monitor")
        }
    }

    @SuppressLint("InlinedApi") // ServiceCompat drops types the running API level does not know.
    private fun foregroundTypes(role: Role): Int {
        val mic = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        val hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        return when (role) {
            Role.EMITTER -> mic
            else -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or (if (hasMic) mic else 0)
        }
    }

    private fun acquireLocks() {
        if (!wakeLock.isHeld) wakeLock.acquire(MAX_SESSION.inWholeMilliseconds)
        if (!wifiLock.isHeld) wifiLock.acquire()
    }

    companion object {
        private const val TAG = "MonitorService"
        private const val EXTRA_ROLE = "role"
        private const val ACTION_STOP = "stop"
        private val MAX_SESSION = 24.hours

        fun start(context: Context, role: Role) = ContextCompat.startForegroundService(
            context, Intent(context, MonitorService::class.java).putExtra(EXTRA_ROLE, role.name),
        )

        fun stop(context: Context) = context.stopService(Intent(context, MonitorService::class.java))

        internal fun stopIntent(context: Context) = Intent(context, MonitorService::class.java).setAction(ACTION_STOP)
    }
}
