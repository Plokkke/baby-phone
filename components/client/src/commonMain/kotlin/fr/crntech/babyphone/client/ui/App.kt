package fr.crntech.babyphone.client.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.crntech.babyphone.client.monitor.EmitterSession
import fr.crntech.babyphone.client.monitor.ReceiverSession
import fr.crntech.babyphone.client.platform.LocalPlatformUi
import fr.crntech.babyphone.client.platform.PlatformUi
import fr.crntech.babyphone.client.resources.Res
import fr.crntech.babyphone.client.resources.pairing_invalid
import fr.crntech.babyphone.client.resources.permission_mic_required
import fr.crntech.babyphone.client.ui.screens.EmitterScreen
import fr.crntech.babyphone.client.ui.screens.HomeScreen
import fr.crntech.babyphone.client.ui.screens.PairingScreen
import fr.crntech.babyphone.client.ui.screens.ReceiverScreen
import org.jetbrains.compose.resources.getString

/** Root of the app, identical on every platform. */
@Composable
fun App(viewModel: AppViewModel, platformUi: PlatformUi) = CompositionLocalProvider(LocalPlatformUi provides platformUi) {
    BabyPhoneTheme {
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(viewModel) {
            viewModel.notices.collect { snackbar.showSnackbar(getString(it.message())) }
        }
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = WindowInsets(0)) { _ ->
            // Phone-shaped column, so wide screens (web) keep the same layout.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxHeight()) { Screens(viewModel, platformUi) }
            }
        }
    }
}

@Composable
private fun Screens(viewModel: AppViewModel, platformUi: PlatformUi) {
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val scan = platformUi.rememberQrScanner(viewModel::onPairingLink)
    val startRole = platformUi.rememberRoleStarter(viewModel::start) { viewModel.notify(Notice.MICROPHONE_REQUIRED) }

    when (val current = screen) {
        is Screen.Pairing -> PairingScreen(current.link, onScan = scan)
        is Screen.Home -> HomeScreen(current.link, current.peers, startRole, scan, viewModel::resetPairing)
        is Screen.Monitoring -> when (val session = current.session) {
            is EmitterSession -> EmitterScreen(session, viewModel::stop)
            is ReceiverSession -> ReceiverScreen(session, viewModel::stop)
        }
    }
}

private val MAX_CONTENT_WIDTH = 480.dp

private fun Notice.message() = when (this) {
    Notice.INVALID_PAIRING_LINK -> Res.string.pairing_invalid
    Notice.MICROPHONE_REQUIRED -> Res.string.permission_mic_required
}
