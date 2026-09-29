#!/bin/sh
# Downloads the TinySoundFont header the Kotlin port follows into $1 (a directory),
# pinned to one upstream commit and checked by digest: a stale or different
# tsf.h would measure or render a different synth than the app ships.
set -eu
TSF_COMMIT=853a0a171759f1ddba0de1442133a75912bbeffa
TSF_SHA256=70d55963c98f60ebb81518eaa1f25d46888d5180eb5f5289fd6b74ffc177d197
mkdir -p "$1"
digest() { shasum -a 256 "$1" | cut -d' ' -f1; }
if [ ! -f "$1/tsf.h" ] || [ "$(digest "$1/tsf.h")" != "$TSF_SHA256" ]; then
  curl -sfL "https://raw.githubusercontent.com/schellingb/TinySoundFont/$TSF_COMMIT/tsf.h" -o "$1/tsf.h.part"
  if [ "$(digest "$1/tsf.h.part")" != "$TSF_SHA256" ]; then
    echo "tsf.h at $TSF_COMMIT does not match its pinned digest" >&2
    rm -f "$1/tsf.h.part"
    exit 1
  fi
  mv "$1/tsf.h.part" "$1/tsf.h"
fi
