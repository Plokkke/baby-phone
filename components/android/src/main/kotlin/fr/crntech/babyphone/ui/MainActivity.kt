package fr.crntech.babyphone.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.crntech.babyphone.R
import fr.crntech.babyphone.monitor.EmitterSession
import fr.crntech.babyphone.monitor.ReceiverSession
import fr.crntech.babyphone.shared.Role
import fr.crntech.babyphone.ui.components.rememberQrScanner
import fr.crntech.babyphone.ui.screens.EmitterScreen
import fr.crntech.babyphone.ui.screens.HomeScreen
import fr.crntech.babyphone.ui.screens.PairingScreen
import fr.crntech.babyphone.ui.screens.ReceiverScreen

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handlePairingIntent(intent)
        setContent { BabyPhoneTheme { Surface { App(viewModel) } } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePairingIntent(intent)
    }

    private fun handlePairingIntent(intent: Intent) {
        val link = intent.data?.toString() ?: return
        if (!viewModel.onPairingLink(link)) toast(R.string.pairing_invalid)
    }
}

@Composable
private fun App(viewModel: AppViewModel) {
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scan = rememberQrScanner { if (!viewModel.onPairingLink(it)) context.toast(R.string.pairing_invalid) }
    val startRole = rememberRoleStarter(viewModel::start)

    when (val current = screen) {
        Screen.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is Screen.Pairing -> PairingScreen(current.link, onScan = scan)
        is Screen.Home -> HomeScreen(current.link, current.peers, startRole, scan, viewModel::resetPairing)
        is Screen.Monitoring -> when (val session = current.session) {
            is EmitterSession -> EmitterScreen(session, viewModel::stop)
            is ReceiverSession -> ReceiverScreen(session, viewModel::stop)
        }
    }
}

/** Asks for microphone (+ notifications) first. The emitter cannot run without the mic; the receiver only loses talk-back. */
@Composable
private fun rememberRoleStarter(start: (Role) -> Unit): (Role) -> Unit {
    val context = LocalContext.current
    val pending = remember { mutableStateOf<Role?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val role = pending.value ?: return@rememberLauncherForActivityResult
        pending.value = null
        val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (role == Role.EMITTER && !micGranted) context.toast(R.string.permission_mic_required) else start(role)
    }
    return { role ->
        pending.value = role
        launcher.launch(RUNTIME_PERMISSIONS)
    }
}

private fun android.content.Context.toast(message: Int) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

private val RUNTIME_PERMISSIONS = listOfNotNull(
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.POST_NOTIFICATIONS.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU },
).toTypedArray()
