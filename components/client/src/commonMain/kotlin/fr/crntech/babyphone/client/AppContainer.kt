package fr.crntech.babyphone.client

import fr.crntech.babyphone.client.data.SettingsStore
import fr.crntech.babyphone.client.monitor.EmitterSession
import fr.crntech.babyphone.client.monitor.MonitorSession
import fr.crntech.babyphone.client.monitor.ReceiverSession
import fr.crntech.babyphone.client.net.PeerLink
import fr.crntech.babyphone.client.net.ServerTransport
import fr.crntech.babyphone.client.platform.Platform
import fr.crntech.babyphone.shared.Endpoints
import fr.crntech.babyphone.shared.PairingLink
import fr.crntech.babyphone.shared.PairingSecret
import fr.crntech.babyphone.shared.Peer
import fr.crntech.babyphone.shared.PeerCodec
import fr.crntech.babyphone.shared.Role

/** Manual dependency wiring. The transport is picked here, so a LAN transport can slot in later. */
class AppContainer(private val platform: Platform) {
    val settings = SettingsStore(platform.storage)

    fun pairingLink(secret: PairingSecret) = PairingLink.build(platform.publicUrl, secret)

    suspend fun peerLink(secret: PairingSecret, deviceId: String, role: Role): PeerLink {
        val self = Peer(deviceId, platform.deviceName, role)
        val url = Endpoints.roomSocket(platform.publicUrl, secret.roomId(), self)
        return PeerLink(ServerTransport(platform.httpClient, url), PeerCodec.create(secret))
    }

    suspend fun createSession(role: Role): MonitorSession {
        val current = settings.data.value
        val link = peerLink(current.secret, current.deviceId, role)
        return when (role) {
            Role.EMITTER -> EmitterSession(link, settings, platform.microphone, platform.speaker, platform.battery, current.thresholdDb)
            Role.RECEIVER -> ReceiverSession(link, platform.microphone, platform.speaker, platform.alarm())
            Role.IDLE -> error("Idle devices do not monitor")
        }
    }
}
