package fr.crntech.babyphone.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.crntech.babyphone.container
import fr.crntech.babyphone.monitor.MonitorHub
import fr.crntech.babyphone.monitor.MonitorService
import fr.crntech.babyphone.monitor.MonitorSession
import fr.crntech.babyphone.shared.PairingLink
import fr.crntech.babyphone.shared.Peer
import fr.crntech.babyphone.shared.Role
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Loading : Screen
    data class Pairing(val link: String) : Screen
    data class Home(val link: String, val peers: List<Peer>) : Screen
    data class Monitoring(val session: MonitorSession) : Screen
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.container
    private val settings = container.settings
    private val otherPeers = MutableStateFlow(emptyList<Peer>())

    val screen = combine(settings.data, MonitorHub.session, otherPeers) { current, session, peers ->
        val link = container.pairingLink(current.secret)
        when {
            session != null -> Screen.Monitoring(session)
            !current.paired -> Screen.Pairing(link)
            else -> Screen.Home(link, peers)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), Screen.Loading)

    init {
        viewModelScope.launch { watchRoomWhileIdle() }
    }

    /** Returns false when the scanned text is not one of our pairing links. */
    fun onPairingLink(link: String): Boolean {
        val secret = PairingLink.parse(link) ?: return false
        MonitorService.stop(getApplication())
        viewModelScope.launch { settings.pairWith(secret) }
        return true
    }

    fun start(role: Role) = MonitorService.start(getApplication(), role)

    fun stop() = MonitorService.stop(getApplication())

    fun resetPairing() {
        viewModelScope.launch { settings.resetPairing() }
    }

    /** Joins the room as IDLE outside monitoring, to see who is paired and detect the first scan. */
    private suspend fun watchRoomWhileIdle() {
        combine(settings.data, MonitorHub.session) { current, session -> current.takeIf { session == null } }
            .distinctUntilChanged { old, new -> old?.secret == new?.secret }
            .collectLatest { current ->
                otherPeers.value = emptyList()
                if (current == null) return@collectLatest
                val link = container.peerLink(current.secret, current.deviceId, Role.IDLE)
                coroutineScope {
                    launch { link.run() }
                    link.presence.collect { peers ->
                        val others = peers.filterNot { it.deviceId == current.deviceId }
                        otherPeers.value = others
                        if (others.isNotEmpty()) settings.markPaired()
                    }
                }
            }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
