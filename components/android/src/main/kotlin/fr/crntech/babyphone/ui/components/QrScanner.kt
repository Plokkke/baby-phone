package fr.crntech.babyphone.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/** Google Play services scanner: no camera permission needed in the app. */
@Composable
fun rememberQrScanner(onScanned: (String) -> Unit): () -> Unit {
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
