package fr.crntech.babyphone.monitor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface MonitorSession {
    /** Runs until cancelled by [MonitorService]. */
    suspend fun run()
}

/** Bridges the running foreground service session to the UI. */
object MonitorHub {
    private val _session = MutableStateFlow<MonitorSession?>(null)
    val session = _session.asStateFlow()

    internal fun publish(session: MonitorSession?) {
        _session.value = session
    }
}
