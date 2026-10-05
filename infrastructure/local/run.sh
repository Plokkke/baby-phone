#!/usr/bin/env bash
# Runs the relay on this computer and installs a debug app pointing at it on every connected phone (USB or wireless adb).
# Phones only need to be on the same Wi-Fi. Override the address with HOST_IP=… when the guess is wrong.
set -euo pipefail

cd "$(dirname "$0")/../.."
host_ip="${HOST_IP:-$(ipconfig getifaddr en0 2>/dev/null || hostname -I | awk '{print $1}')}"
port="${PORT:-8080}"
url="http://${host_ip}:${port}"
adb="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
# A separate app: installs next to the released one, with its own pairing and no version conflict.
app_id="fr.crntech.babyphone.local"
# The commit in the version tells on the phone which build is installed.
version="0.0.0-dev.$(git rev-parse --short HEAD)$(git diff --quiet HEAD || echo .modified)"
gradle_args=(-Pbabyphone.publicUrl="$url" -Pbabyphone.applicationId="$app_id" -Pbabyphone.version="$version")

if "$adb" devices | grep -qw device$; then
    ./gradlew :android:installDebug "${gradle_args[@]}"
else
    ./gradlew :android:assembleDebug "${gradle_args[@]}"
    echo "No phone connected to adb: install components/android/build/outputs/apk/debug/android-debug.apk by hand,"
    echo "or connect one (USB, or 'adb connect <ip>:<port>' from the phone's Wireless debugging screen) and run this again."
fi

echo "Relay on $url — web client on http://localhost:${port}/web/ from this computer"
PORT="$port" ANDROID_PACKAGE="$app_id" APP_VERSION="$version" ./gradlew :server:run
