package fr.crntech.babyphone.client.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.crntech.babyphone.client.AppContainer
import fr.crntech.babyphone.client.monitor.MonitorController
import fr.crntech.babyphone.client.monitor.MonitorSession
import fr.crntech.babyphone.shared.PairingLink
import fr.crntech.babyphone.shared.Peer
import fr.crntech.babyphone.shared.Role
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Screen {
    data class Pairing(val link: String) : Screen
    data class Home(val link: String, val peers: List<Peer>) : Screen
    data class Monitoring(val session: MonitorSession, val link: String) : Screen
}

enum class Notice { INVALID_PAIRING_LINK, MICROPHONE_REQUIRED }

class AppViewModel(private val container: AppContainer, private val monitor: MonitorController) : ViewModel() {
    private val settings = container.settings
    val appVersion = container.appVersion
    private val otherPeers = MutableStateFlow(emptyList<Peer>())
    private val _notices = Channel<Notice>(Channel.BUFFERED)
    val notices = _notices.receiveAsFlow()

    val screen = combine(settings.data, monitor.session, otherPeers) { current, session, peers ->
        val link = container.pairingLink(current.secret)
        when {
            session != null -> Screen.Monitoring(session, link)
            !current.paired -> Screen.Pairing(link)
            else -> Screen.Home(link, peers)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Screen.Pairing(container.pairingLink(settings.data.value.secret)))

    init {
        viewModelScope.launch { watchRoomWhileIdle() }
    }

    fun onPairingLink(link: String) {
        val secret = PairingLink.parse(link) ?: return notify(Notice.INVALID_PAIRING_LINK)
        monitor.stop()
        settings.pairWith(secret)
    }

    fun start(role: Role) = monitor.start(role)

    fun stop() = monitor.stop()

    fun resetPairing() = settings.resetPairing()

    fun notify(notice: Notice) {
        _notices.trySend(notice)
    }

    /** Joins the room as IDLE outside monitoring, to see who is paired and detect the first scan. */
    private suspend fun watchRoomWhileIdle() {
        combine(settings.data, monitor.session) { current, session -> current.takeIf { session == null } }
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
}
