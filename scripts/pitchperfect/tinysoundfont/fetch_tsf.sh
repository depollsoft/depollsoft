#!/bin/sh
# Downloads the TinySoundFont header the Kotlin port follows into $1 (a directory).
set -eu
TSF_COMMIT=853a0a171759f1ddba0de1442133a75912bbeffa
mkdir -p "$1"
[ -f "$1/tsf.h" ] || curl -sfL "https://raw.githubusercontent.com/schellingb/TinySoundFont/$TSF_COMMIT/tsf.h" -o "$1/tsf.h"
