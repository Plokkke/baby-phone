// AudioWorklet: resamples the microphone to 16 kHz mono PCM16 and posts 20 ms frames (320 samples).
const TARGET_RATE = 16000;
const FRAME_SAMPLES = 320;

class PcmCapture extends AudioWorkletProcessor {
  constructor() {
    super();
    this.step = sampleRate / TARGET_RATE;
    this.position = 0;
    this.frame = new Int16Array(FRAME_SAMPLES);
    this.filled = 0;
  }

  process(inputs) {
    const channel = inputs[0][0];
    if (!channel) return true;
    for (; this.position < channel.length; this.position += this.step) {
      const sample = Math.max(-1, Math.min(1, channel[Math.floor(this.position)]));
      this.frame[this.filled++] = sample * 0x7fff;
      if (this.filled === FRAME_SAMPLES) {
        this.port.postMessage(this.frame.buffer, [this.frame.buffer]);
        this.frame = new Int16Array(FRAME_SAMPLES);
        this.filled = 0;
      }
    }
    this.position -= channel.length;
    return true;
  }
}

registerProcessor('pcm-capture', PcmCapture);
