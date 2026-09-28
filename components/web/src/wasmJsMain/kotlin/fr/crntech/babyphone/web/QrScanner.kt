@file:OptIn(ExperimentalWasmJsInterop::class)

package fr.crntech.babyphone.web

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.mediacapture.MediaStream
import org.w3c.dom.mediacapture.MediaStreamConstraints
import kotlin.time.Duration.Companion.milliseconds

/** Webcam QR scanner drawn as an HTML overlay above the Compose canvas. */
object QrScanner {
    val available: Boolean get() = hasCamera()

    fun open(onScanned: (String) -> Unit) {
        val overlay = element<HTMLDivElement>("div").apply { className = "scanner" }
        val video = element<HTMLVideoElement>("video").apply {
            muted = true
            setAttribute("playsinline", "")
        }
        val cancel = element<HTMLButtonElement>("button").apply { textContent = "Annuler" }
        overlay.append(video, cancel)
        document.body!!.append(overlay)

        val scan = MainScope().launch {
            val stream = window.navigator.mediaDevices
                .getUserMedia(rearCameraConstraints().unsafeCast<MediaStreamConstraints>())
                .await<MediaStream>()
            try {
                video.srcObject = stream
                video.play()
                onScanned(firstQrCode(video))
            } finally {
                stream.getTracks().toList().forEach { it.stop() }
            }
        }
        scan.invokeOnCompletion { overlay.remove() }
        cancel.onclick = { scan.cancel() }
    }

    private suspend fun firstQrCode(video: HTMLVideoElement): String {
        val detector = nativeQrDetector()
        val canvas = element<HTMLCanvasElement>("canvas")
        while (true) {
            delay(SCAN_INTERVAL)
            if (video.videoWidth == 0) continue
            val found = if (detector != null) detectNatively(detector, video) else detectWithJsQr(canvas, video)
            if (found != null) return found
        }
    }

    private suspend fun detectNatively(detector: BarcodeDetector, video: HTMLVideoElement): String? {
        val codes = detector.detect(video).await<JsArray<DetectedBarcode>>()
        return if (codes.length > 0) codes[0]?.rawValue else null
    }

    private fun detectWithJsQr(canvas: HTMLCanvasElement, video: HTMLVideoElement): String? {
        canvas.width = video.videoWidth
        canvas.height = video.videoHeight
        val context = canvas.getContext("2d") as CanvasRenderingContext2D
        context.drawImage(video, 0.0, 0.0)
        val image = context.getImageData(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
        return jsQR(image.data, image.width, image.height)?.data
    }

    private fun <T : JsAny> element(tag: String): T = document.createElement(tag).unsafeCast<T>()

    private val SCAN_INTERVAL = 200.milliseconds
}

private fun hasCamera(): Boolean = js("!!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia)")
