@file:OptIn(ExperimentalWasmJsInterop::class)

package fr.crntech.babyphone.web

import fr.crntech.babyphone.client.AppContainer
import fr.crntech.babyphone.client.monitor.MonitorController
import fr.crntech.babyphone.client.monitor.MonitorSession
import fr.crntech.babyphone.shared.Role
import kotlinx.browser.document
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Runs the session inside the tab. A screen wake lock keeps the computer from sleeping;
 * browsers drop it when the tab is hidden, so it is requested again when it comes back.
 */
class BrowserMonitorController(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    private val onFailure: () -> Unit,
) : MonitorController {
    private val _session = MutableStateFlow<MonitorSession?>(null)
    override val session = _session.asStateFlow()

    private var job: Job? = null
    private var wakeLock: WakeLockSentinel? = null

    init {
        document.addEventListener("visibilitychange", {
            if (isPageVisible() && _session.value != null) scope.launch { keepAwake() }
        })
    }

    override fun start(role: Role) {
        job?.cancel()
        job = scope.launch {
            val session = container.createSession(role)
            _session.value = session
            keepAwake()
            try {
                session.run()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("Monitoring stopped: ${e.message}")
                onFailure()
            } finally {
                _session.value = null
                wakeLock?.release()
                wakeLock = null
            }
        }
    }

    override fun stop() {
        job?.cancel()
    }

    private suspend fun keepAwake() {
        wakeLock = runCatching { requestScreenWakeLock()?.await<WakeLockSentinel>() }.getOrNull()
    }
}

private fun isPageVisible(): Boolean = js("document.visibilityState === 'visible'")
