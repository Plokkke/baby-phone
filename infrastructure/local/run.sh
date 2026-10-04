#!/usr/bin/env bash
# Runs the relay on this computer and installs a debug app pointing at it on every connected phone (USB or wireless adb).
# Phones only need to be on the same Wi-Fi. Override the address with HOST_IP=… when the guess is wrong.
set -euo pipefail

cd "$(dirname "$0")/../.."
host_ip="${HOST_IP:-$(ipconfig getifaddr en0 2>/dev/null || hostname -I | awk '{print $1}')}"
url="http://${host_ip}:${PORT:-8080}"

./gradlew :android:installDebug -Pbabyphone.publicUrl="$url"
echo "Relay on $url — web client on http://localhost:${PORT:-8080}/web/ from this computer"
./gradlew :server:run
