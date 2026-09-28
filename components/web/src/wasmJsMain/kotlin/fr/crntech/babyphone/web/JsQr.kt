@file:JsModule("jsqr")
@file:OptIn(ExperimentalWasmJsInterop::class)

package fr.crntech.babyphone.web

import org.khronos.webgl.Uint8ClampedArray

external interface JsQrCode : JsAny {
    val data: String
}

/** Pure JavaScript QR decoder, used when the browser has no BarcodeDetector. */
@JsName("default")
external fun jsQR(data: Uint8ClampedArray, width: Int, height: Int): JsQrCode?
