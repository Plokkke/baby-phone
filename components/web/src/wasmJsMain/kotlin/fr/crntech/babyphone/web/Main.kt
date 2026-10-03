@file:OptIn(ExperimentalWasmJsInterop::class)

package fr.crntech.babyphone.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import fr.crntech.babyphone.client.AppContainer
import fr.crntech.babyphone.client.platform.PlatformUi
import fr.crntech.babyphone.client.ui.App
import fr.crntech.babyphone.client.ui.AppViewModel
import fr.crntech.babyphone.client.ui.Notice
import fr.crntech.babyphone.shared.PairingLink
import fr.crntech.babyphone.shared.Role
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import org.w3c.notifications.Notification

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val container = AppContainer(browserPlatform())
    lateinit var viewModel: AppViewModel
    val monitor = BrowserMonitorController(container, MainScope()) { viewModel.notify(Notice.MICROPHONE_REQUIRED) }
    viewModel = AppViewModel(container, monitor)
    pairFromUrl(viewModel)
    ComposeViewport(document.body!!) { App(viewModel, BrowserPlatformUi) }
}

/** `/pair#s=…` redirects here with its fragment: join that pairing, then wipe the secret from the address bar. */
private fun pairFromUrl(viewModel: AppViewModel) {
    val fragment = window.location.hash
    if (fragment.length <= 1) return
    viewModel.onPairingLink(window.location.origin + PairingLink.PATH + fragment)
    window.history.replaceState(null, "", window.location.pathname)
}

private object BrowserPlatformUi : PlatformUi {
    @Composable
    override fun rememberQrScanner(onScanned: (String) -> Unit): (() -> Unit)? =
        if (QrScanner.available) { { QrScanner.open(onScanned) } } else null

    @Composable
    override fun rememberCopier(): (String) -> Unit = ::writeClipboard

    /** Runs inside the click: browsers only open the share sheet on a user gesture. */
    @Composable
    override fun rememberSharer(): ((String) -> Unit)? = if (canShare()) ::shareUrl else null

    /** Runs inside the click: the only moment browsers allow starting audio and asking for notifications. */
    @Composable
    override fun rememberRoleStarter(start: (Role) -> Unit, onMicrophoneDenied: () -> Unit): (Role) -> Unit = { role ->
        WebAudio.unlock()
        Notification.requestPermission()
        start(role)
    }

    /** The emitter screen is already black; nothing more a page can do. */
    @Composable
    override fun NightScreen() = Unit
}
