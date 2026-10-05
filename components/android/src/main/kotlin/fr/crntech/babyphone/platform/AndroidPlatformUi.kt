package fr.crntech.babyphone.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PersistableBundle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import fr.crntech.babyphone.BuildConfig
import fr.crntech.babyphone.client.platform.PlatformUi
import fr.crntech.babyphone.shared.Role

object AndroidPlatformUi : PlatformUi {

    /** Google Play services scanner: no camera permission needed in the app. */
    @Composable
    override fun rememberQrScanner(onScanned: (String) -> Unit): (() -> Unit)? {
        val context = LocalContext.current
        val callback = rememberUpdatedState(onScanned)
        return remember(context) {
            {
                val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
                GmsBarcodeScanning.getClient(context, options).startScan()
                    .addOnSuccessListener { barcode -> barcode.rawValue?.let(callback.value) }
            }
        }
    }

    /** The sensitive flag is an extra older Android versions simply ignore. */
    @SuppressLint("InlinedApi")
    @Composable
    override fun rememberCopier(): (String) -> Unit {
        val context = LocalContext.current
        return remember(context) {
            { text ->
                val clip = ClipData.newPlainText(null, text).apply {
                    description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
                }
                context.getSystemService<ClipboardManager>()?.setPrimaryClip(clip)
            }
        }
    }

    @Composable
    override fun rememberSharer(): (String) -> Unit {
        val context = LocalContext.current
        return remember(context) {
            { text ->
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                context.startActivity(Intent.createChooser(send, null))
            }
        }
    }

    /** The emitter cannot run without the mic; the receiver only loses talk-back. */
    @Composable
    override fun rememberRoleStarter(start: (Role) -> Unit, onMicrophoneDenied: () -> Unit): (Role) -> Unit {
        val context = LocalContext.current
        val pending = remember { mutableStateOf<Role?>(null) }
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val role = pending.value ?: return@rememberLauncherForActivityResult
            pending.value = null
            val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (role == Role.EMITTER && !micGranted) onMicrophoneDenied() else start(role)
        }
        return { role ->
            pending.value = role
            launcher.launch(RUNTIME_PERMISSIONS)
        }
    }

    @Composable
    override fun NightScreen() {
        // Debug builds are tested in daylight: keep the screen readable.
        if (BuildConfig.DEBUG) return
        val window = LocalActivity.current?.window ?: return
        DisposableEffect(window) {
            val previous = window.attributes.screenBrightness
            window.attributes = window.attributes.apply { screenBrightness = 0.01f }
            onDispose {
                window.attributes = window.attributes.apply { screenBrightness = previous }
            }
        }
    }

    private val RUNTIME_PERMISSIONS = listOfNotNull(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU },
    ).toTypedArray()
}
