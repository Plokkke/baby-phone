package fr.crntech.babyphone

import android.content.Context
import android.os.Build
import fr.crntech.babyphone.data.SettingsStore
import fr.crntech.babyphone.net.PeerLink
import fr.crntech.babyphone.net.ServerTransport
import fr.crntech.babyphone.shared.Endpoints
import fr.crntech.babyphone.shared.PairingLink
import fr.crntech.babyphone.shared.PairingSecret
import fr.crntech.babyphone.shared.PeerCodec
import fr.crntech.babyphone.shared.Peer
import fr.crntech.babyphone.shared.Role
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import kotlin.time.Duration.Companion.seconds

/** Manual dependency wiring. The transport is picked here, so a LAN transport can slot in later. */
class AppContainer(context: Context) {
    val settings = SettingsStore(context)

    private val httpClient by lazy {
        HttpClient(OkHttp) {
            install(WebSockets) { pingInterval = 5.seconds }
        }
    }

    fun pairingLink(secret: PairingSecret) = PairingLink.build(BuildConfig.PUBLIC_URL, secret)

    fun peerLink(secret: PairingSecret, deviceId: String, role: Role): PeerLink {
        val self = Peer(deviceId, Build.MODEL, role)
        val url = Endpoints.roomSocket(BuildConfig.PUBLIC_URL, secret.roomId, self)
        return PeerLink(ServerTransport(httpClient, url), PeerCodec(secret))
    }
}

val Context.container get() = (applicationContext as BabyPhoneApp).container
