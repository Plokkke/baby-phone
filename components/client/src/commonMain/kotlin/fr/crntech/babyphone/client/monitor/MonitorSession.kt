package fr.crntech.babyphone.client.monitor

import fr.crntech.babyphone.shared.Peer
import fr.crntech.babyphone.shared.Role
import kotlinx.coroutines.flow.StateFlow

sealed interface MonitorSession {
    /** Devices connected to the pairing, this one included. */
    val presence: StateFlow<List<Peer>>

    /** Runs until cancelled. */
    suspend fun run()
}

/** Starts and stops monitoring the way the platform requires (foreground service, browser tab...). */
interface MonitorController {
    val session: StateFlow<MonitorSession?>
    fun start(role: Role)
    fun stop()
}
