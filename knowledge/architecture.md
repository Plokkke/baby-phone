# Architecture & decisions

## Flow

```
 Emitter (baby room)                     Server (Ktor)                     Receivers (parents)
 mic → dBFS → SoundGate ──E2E frame──▶  /ws/{roomId}  relay by role ──▶  speaker + level meter
      ▲                                  (memory only)                        │
      └──────── talk-back audio, SetThreshold, ForceListen ◀──────────────────┘
```

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
- Status every 250 ms (peak level, threshold, battery), even when quiet → heartbeat.
- **Hold to listen**: the receiver sends `ForceListen` every 500 ms while pressed;
  the emitter opens the gate for 1.5 s after each one, so a lost finger/connection closes it.
- **Talk-back**: toggle, whole screen turns red, auto-off after 60 s.
  Half-duplex on the emitter: while parent audio plays (+300 ms) it does not transmit, to avoid echo.

## Reliability
- Receiver alarm (looping alarm sound + vibration + notification) when no emitter status for 10 s,
  armed once the emitter has been heard at least once.
- Foreground service (microphone / mediaPlayback) + partial wake lock + low-latency Wi-Fi lock.
- Transport reconnects forever (2 s delay), WebSocket ping every 5 s.

## Extension points / roadmap
- `net/Transport` is the seam for a **LAN mode** (NSD discovery, emitter hosts a local WebSocket):
  `PeerLink`, sessions and UI stay unchanged. Pick the transport in `AppContainer.peerLink`.
- Opus via `MediaCodec` (API 29+) would cut bandwidth ~10×; plug it between `Microphone` and `PeerMessage.Audio`.
- Acoustic echo cancellation for full-duplex talk-back.
- Cry detection (TFLite YAMNet) as an alternative trigger to the dB threshold.
- Push (FCM) wake-up of receivers when the app is killed.
