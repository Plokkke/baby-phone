@file:OptIn(ExperimentalWasmJsInterop::class)

package fr.crntech.babyphone.web

import org.khronos.webgl.Float32Array
import org.w3c.dom.MessagePort
import org.w3c.dom.mediacapture.MediaStream
import kotlin.js.Promise

// Browser APIs missing from kotlinx-browser, declared for what the app uses only.

external class AudioContext : JsAny {
    val currentTime: Double
    val destination: AudioNode
    val audioWorklet: AudioWorklet
    val state: String
    fun resume(): Promise<JsAny?>
    fun createMediaStreamSource(stream: MediaStream): AudioNode
    fun createBiquadFilter(): BiquadFilterNode
    fun createGain(): GainNode
    fun createOscillator(): OscillatorNode
    fun createBuffer(numberOfChannels: Int, length: Int, sampleRate: Float): AudioBuffer
    fun createBufferSource(): AudioBufferSourceNode
}

external interface AudioWorklet : JsAny {
    fun addModule(url: String): Promise<JsAny?>
}

open external class AudioNode : JsAny {
    fun connect(destination: AudioNode): AudioNode
    fun disconnect()
}

external interface AudioParam : JsAny {
    var value: Float
}

external class BiquadFilterNode : AudioNode {
    var type: String
    val frequency: AudioParam
}

external class GainNode : AudioNode {
    val gain: AudioParam
}

external class OscillatorNode : AudioNode {
    var type: String
    val frequency: AudioParam
    fun start(`when`: Double)
    fun stop(`when`: Double)
}

external class AudioBuffer : JsAny {
    fun getChannelData(channel: Int): Float32Array
}

external class AudioBufferSourceNode : AudioNode {
    var buffer: AudioBuffer?
    fun start(`when`: Double)
}

external class AudioWorkletNode(context: AudioContext, name: String) : AudioNode {
    val port: MessagePort
}

external interface BatteryManager : JsAny {
    val level: Double
    val charging: Boolean
}

external interface WakeLockSentinel : JsAny {
    fun release(): Promise<JsAny?>
}

external interface BarcodeDetector : JsAny {
    fun detect(source: JsAny): Promise<JsArray<DetectedBarcode>>
}

external interface DetectedBarcode : JsAny {
    val rawValue: String
}

internal fun getBattery(): Promise<BatteryManager>? = js("navigator.getBattery ? navigator.getBattery() : null")

internal fun requestScreenWakeLock(): Promise<WakeLockSentinel>? =
    js("navigator.wakeLock ? navigator.wakeLock.request('screen') : null")

/** Native detector (Chrome on macOS/ChromeOS/Android); null elsewhere, where jsQR takes over. */
internal fun nativeQrDetector(): BarcodeDetector? =
    js("('BarcodeDetector' in window) ? new BarcodeDetector({ formats: ['qr_code'] }) : null")

internal fun microphoneConstraints(processed: Boolean): JsAny =
    js("({ audio: { channelCount: 1, echoCancellation: processed, noiseSuppression: processed, autoGainControl: processed } })")

internal fun rearCameraConstraints(): JsAny = js("({ video: { facingMode: 'environment' }, audio: false })")
