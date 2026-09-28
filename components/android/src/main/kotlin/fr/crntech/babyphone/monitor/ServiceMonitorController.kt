package fr.crntech.babyphone.monitor

import android.content.Context
import fr.crntech.babyphone.client.monitor.MonitorController
import fr.crntech.babyphone.client.monitor.MonitorSession
import fr.crntech.babyphone.shared.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Bridges the session running in [MonitorService] to the UI. */
internal object MonitorHub {
    private val _session = MutableStateFlow<MonitorSession?>(null)
    val session = _session.asStateFlow()

    fun publish(session: MonitorSession?) {
        _session.value = session
    }
}

/** Monitoring runs in a foreground service so it survives the screen being off. */
class ServiceMonitorController(private val context: Context) : MonitorController {
    override val session = MonitorHub.session

    override fun start(role: Role) = MonitorService.start(context, role)

    override fun stop() {
        MonitorService.stop(context)
    }
}
