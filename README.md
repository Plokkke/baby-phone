# Baby Phone

Android baby monitor + Kotlin relay server, in one Gradle monorepo.

```
components/
├── shared/    Kotlin Multiplatform (JVM + Wasm): protocol, pairing, E2E crypto, sound gate
├── server/    Ktor WebSocket relay — forwards encrypted frames in memory, stores nothing
├── client/    Kotlin Multiplatform (Android + Wasm): sessions, transport, settings, Compose UI
├── android/   Android shell: activity, foreground service, audio/battery/alarm implementations
└── web/       Browser shell (Wasm), bundled in the server jar and served under /web
infrastructure/ Dockerfile, Terraform (RPi deploy, Play), bootstrap state DB, nginx site
knowledge/      Architecture & design decisions
```

## Build & test

```bash
./gradlew :shared:allTests :server:test    # unit (JVM + Wasm) + WebSocket integration tests
./gradlew :android:assembleDebug           # needs an Android SDK (local.properties → sdk.dir)
./gradlew :android:installDebug            # on a USB-connected phone
./gradlew :server:run                      # local server on :8080, web client on /web/
```

The public URL lives in one place: `babyphone.publicUrl` in `gradle.properties`.
It feeds the QR links, the App Links host and the WebSocket endpoint.

## Delivery

PRs are validated by GitHub Actions; merging to `main` cuts a semver release, pushes a multi-arch image
to GHCR and deploys it with Terraform on the Raspberry Pi. See [knowledge/delivery.md](knowledge/delivery.md)
and [knowledge/play-store.md](knowledge/play-store.md).

## Using it

1. Launch the app → a pairing QR code is displayed.
2. Scan it with the other phone's **system camera** (App Link opens the app) or the in-app scanner.
3. Each phone picks its role: 👶🌙 near the baby, 👥💬 with the parents.
4. More receivers: *Ajouter un appareil* on any paired phone shows the QR again.
