# Architecture & decisions

## Flow

```
 Emitter (baby room)                     Server (Ktor)                     Receivers (parents)
 mic → dBFS → SoundGate ──E2E frame──▶  /ws/{roomId}  relay by role ──▶  speaker + level meter
      ▲                                  (memory only)                        │
      └──────── talk-back audio, SetThreshold, ForceListen ◀──────────────────┘
```

## Modules

```
shared (KMP: jvm, wasmJs) ◀── server (JVM)
   ▲
client (KMP: android, wasmJs)   commonMain only: no expect/actual
   ▲
android (app shell)             implements client.platform.* (mic, speaker, battery, alarm, storage, UI hooks)
web (Wasm shell)                same contracts with Web Audio, localStorage, Wake Lock, webcam scanner
```
Everything a device does (sessions, transport, settings, screens) lives once in `client`.
A platform only provides a `Platform` (hardware + storage + HTTP client), a `PlatformUi`
(scanner, permissions, night screen) and a `MonitorController` (foreground service on Android).

Crypto goes through `cryptography-kotlin` (JDK provider on JVM/Android, Web Crypto in browsers),
hence the suspending `PeerCodec` / `PairingSecret.roomId()`. The frame format is unchanged
(`nonce(12) || ciphertext+tag`) and a reference vector pins the room id derivation.

## Web client (PC)
- Served by the relay under `/web/` (bundle embedded in the jar, gzip, content-hashed `.wasm` cached for a year).
- A full device: emitter or receiver. Mic through an AudioWorklet resampling to 16 kHz (`pcm-capture.js`),
  playback by scheduling AudioBuffers (latency capped at 0.5 s), alarm = beeps + system notification.
- Scans with `BarcodeDetector` when the browser has it, else jsQR on webcam frames.
- Limits: the tab must stay open; a screen Wake Lock keeps the computer awake.
- Routing by device (`User-Agent`, `Sec-CH-UA-Mobile`, `Sec-Fetch-Mode`):
  - `/pair` on desktop → 302 `/web/` (the `#s=` fragment survives redirects, so the PC pairs itself);
  - `/pair` on Android → `intent://…/pair?s=…` opening the app, else `PLAY_STORE_URL` or an install notice;
  - `/pair` on iOS → message; `/web` on mobile → back to `/pair`.

## Pairing & security
- QR = `https://<host>/pair#s=<32-byte secret>`. The secret sits in the URL **fragment**:
  a browser opening the link never sends it to the server.
- `roomId = HMAC(secret, "room")` is all the server sees. `key = HMAC(secret, "aes-gcm")`.
- Every peer message is CBOR + AES-256-GCM with a random nonce. The server relays opaque bytes;
  it cannot listen, and it cannot forge messages (GCM tag).
- Presence (who is connected, which role) is the only plaintext the server produces.
- Anyone can re-scan the QR to add a receiver. "Réinitialiser l'appairage" rotates the secret.

## Routing (server)
- `EMITTER → RECEIVER`, `RECEIVER → EMITTER`, `IDLE → nobody` (`Role.audience`).
- Each member has a 64-frame outbox dropping the oldest frames: a slow phone never delays the others.

## Audio
- 16 kHz mono PCM16, 20 ms frames (~256 kbit/s while transmitting, nothing when quiet).
- `SoundGate`: 2 s pre-roll (the start of a cry is not lost) + 5 s hangover.
- Status every 250 ms (peak level, threshold, battery, do-not-disturb), even when quiet → heartbeat.
- CBOR decoding ignores unknown fields, so newer apps can add status fields without breaking older ones.
- **Hold to listen**: the receiver sends `ForceListen` every 500 ms while pressed;
  the emitter opens the gate for 1.5 s after each one, so a lost finger/connection closes it.
- **Talk-back**: toggle, whole screen turns red, auto-off after 60 s.
  Half-duplex on the emitter: while parent audio plays (+300 ms) it does not transmit, to avoid echo.

## Reliability
- Receiver alarm (looping alarm sound + vibration + notification) when no emitter status for 10 s,
  armed once the emitter has been heard at least once.
- Foreground service (microphone / mediaPlayback) + partial wake lock + low-latency Wi-Fi lock.
- Transport reconnects forever (2 s delay), WebSocket ping every 5 s.

## Sound safety
- **Emitter**: turns do-not-disturb on when monitoring starts and restores it on stop, with an in-app switch.
  Android needs the one-time "Do Not Disturb access" (the switch opens that system screen). The app uses the
  `ALARMS` filter: `NONE` (total silence) would also mute the parents talking back. The state travels in
  `EmitterStatus.quiet` and the receiver warns when the baby's phone is not silenced.
- **Receiver**: watches media + alarm volumes (baby audio, lost-link alert), total-silence DND and disabled
  notifications; raises volumes at start and offers "Monter le volume" / "Tester le son".
- **Browsers** cannot read the system volume nor touch DND: only "audio blocked until a click" and
  "notifications denied" are detected, plus a test chime.

## Extension points / roadmap
- `net/Transport` is the seam for a **LAN mode** (NSD discovery, emitter hosts a local WebSocket):
  `PeerLink`, sessions and UI stay unchanged. Pick the transport in `AppContainer.peerLink`.
- Opus via `MediaCodec` (API 29+) would cut bandwidth ~10×; plug it between `Microphone` and `PeerMessage.Audio`.
- Acoustic echo cancellation for full-duplex talk-back.
- Cry detection (TFLite YAMNet) as an alternative trigger to the dB threshold.
- Push (FCM) wake-up of receivers when the app is killed.
