#!/usr/bin/env bash
# Runs the Firestore + Auth emulators that the app sync tests talk to.
#
#   scripts/firestore-emulator.sh pitchperfect   # runs in the foreground; Ctrl-C to stop
#   scripts/firestore-emulator.sh tagmaster
#
# Firestore listens on localhost:8080 and Auth on localhost:9099 (see
# Firebase/<app>/firebase.json). The project id is the reserved demo one for the
# app, so nothing here can reach a production project even with real credentials
# present. The sync tests on both platforms skip themselves when port 8080 is not
# listening, so this script is only needed when you want to run them.
set -euo pipefail

app="${1:-}"
case "$app" in
  pitchperfect|tagmaster) ;;
  *) echo "usage: $0 <pitchperfect|tagmaster> [firebase emulators:start args]" >&2; exit 2 ;;
esac
shift

# firebase-tools starts the Firestore emulator through a bundled JAR and needs a
# JDK 21 or newer; the system Java on these machines is older.
# (JAVA_HOME is overridden unconditionally: the shell profile here points at JDK 17.)
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH:$HOME/.local/bin"

cd "$(dirname "$0")/../Firebase/$app"
exec firebase emulators:start --only firestore,auth --project "demo-$app" "$@"
