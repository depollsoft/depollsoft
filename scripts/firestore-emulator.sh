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
# JDK 21 or newer. The caller's JAVA_HOME is kept when it already is one;
# otherwise the usual JDK 21 locations are tried, since the shell profile on
# the development Macs points at JDK 17 for Gradle.
java_major() { "$1/bin/java" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1; }
jdk_ok() { [ -x "$1/bin/java" ] && [ "$(java_major "$1")" -ge 21 ] 2>/dev/null; }
if ! jdk_ok "${JAVA_HOME:-}"; then
  for candidate in \
    /opt/homebrew/opt/openjdk@21 \
    /usr/local/opt/openjdk@21 \
    "$( [ -x /usr/libexec/java_home ] && /usr/libexec/java_home -v 21 2>/dev/null )" \
    /usr/lib/jvm/java-21-openjdk-amd64 \
    /usr/lib/jvm/java-21-openjdk \
    /usr/lib/jvm/temurin-21-jdk-amd64; do
    if [ -n "$candidate" ] && jdk_ok "$candidate"; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi
if ! jdk_ok "${JAVA_HOME:-}"; then
  echo "firestore-emulator.sh: no JDK 21 or newer found; set JAVA_HOME to one" >&2
  exit 1
fi
export PATH="$JAVA_HOME/bin:$PATH:$HOME/.local/bin"

cd "$(dirname "$0")/../Firebase/$app"

# FIRESTORE_EMULATOR_HOST / FIREBASE_AUTH_EMULATOR_HOST (the variables the Firebase SDKs and the
# sync tests read) move the emulators off the default ports, e.g. when 8080 is taken:
#   FIRESTORE_EMULATOR_HOST=localhost:8180 FIREBASE_AUTH_EMULATOR_HOST=localhost:9199 \
#     scripts/firestore-emulator.sh tagmaster
firestore_port="${FIRESTORE_EMULATOR_HOST##*:}"
auth_port="${FIREBASE_AUTH_EMULATOR_HOST##*:}"
config=firebase.json
if [ -n "$firestore_port" ] || [ -n "$auth_port" ]; then
  config=".firebase.emulator-ports.json"
  trap 'rm -f "$config"' EXIT
  python3 - "$firestore_port" "$auth_port" "$config" <<'PY'
import json, sys
firestore, auth, out = sys.argv[1:4]
config = json.load(open("firebase.json"))
if firestore: config["emulators"]["firestore"]["port"] = int(firestore)
if auth: config["emulators"]["auth"]["port"] = int(auth)
json.dump(config, open(out, "w"), indent=2)
PY
fi
firebase emulators:start --config "$config" --only firestore,auth --project "demo-$app" "$@"
