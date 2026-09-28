# Baby Phone

Android baby monitor + Kotlin relay server, in one Gradle monorepo.

```
components/
├── shared/    Pure Kotlin/JVM: protocol, pairing, E2E crypto, sound gate (used by both sides)
├── server/    Ktor WebSocket relay — forwards encrypted frames in memory, stores nothing
└── android/   Jetpack Compose app (emitter + receiver in the same APK)
infrastructure/ Dockerfile, docker-compose (server + Caddy for HTTPS)
knowledge/      Architecture & design decisions
```

## Build & test

```bash
./gradlew :shared:test :server:test        # unit + WebSocket integration tests
./gradlew :android:assembleDebug           # needs an Android SDK (local.properties → sdk.dir)
./gradlew :android:installDebug            # on a USB-connected phone
./gradlew :server:run                      # local server on :8080
```

The public URL lives in one place: `babyphone.publicUrl` in `gradle.properties`.
It feeds the QR links, the App Links host and the WebSocket endpoint.

## Deploy the server

```bash
cp infrastructure/.env.example infrastructure/.env   # set PUBLIC_HOST + signing fingerprints
docker compose -f infrastructure/docker-compose.yml up -d --build
```

DNS `babyphone.crn-tech.fr` → VPS, ports 80/443 open. Caddy fetches the TLS certificate.
Check `https://babyphone.crn-tech.fr/.well-known/assetlinks.json`, then on a phone:
`adb shell pm verify-app-links --re-verify fr.crntech.babyphone`.

Signing fingerprint (debug):
`keytool -list -v -keystore ~/.android/debug.keystore -storepass android | grep SHA`

## Using it

1. Launch the app → a pairing QR code is displayed.
2. Scan it with the other phone's **system camera** (App Link opens the app) or the in-app scanner.
3. Each phone picks its role: 👶🌙 near the baby, 👥💬 with the parents.
4. More receivers: *Ajouter un appareil* on any paired phone shows the QR again.
