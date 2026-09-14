#!/bin/bash
set -euo pipefail
export ANDROID_SERIAL=emulator-5554
prefix=.impeccable/review/tagmaster-android-round3
mode=${1:-screens}
night=${2:-no}
suffix=${3:-}
adb shell cmd uimode night "$night" >/dev/null
adb shell settings put system font_scale "${4:-1.0}"
sleep 1
home() {
  adb shell am force-stop depollsoft.tagmaster
  adb shell am start -n depollsoft.tagmaster/.MeActivity >/dev/null
  sleep 2
}
tap() { adb shell input tap "$1" "$2"; sleep 1; }
tapid() {
  adb shell uiautomator dump /sdcard/tm-round3-ui.xml >/dev/null
  adb exec-out cat /sdcard/tm-round3-ui.xml > /tmp/tm-round3-ui.xml
  coords=$(ID="$1" perl -ne 'if (/resource-id="depollsoft.tagmaster:id\/$ENV{ID}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/) { print int(($1+$3)/2), " ", int(($2+$4)/2) }' /tmp/tm-round3-ui.xml)
  test -n "$coords" || { echo "Missing view $1"; exit 2; }
  adb shell input tap $coords
  sleep 2
}
lost() {
  adb shell am start -W -n depollsoft.tagmaster/.UrlHandlerActivity -a android.intent.action.VIEW -d 'https://tags.depoll.com/tag.php?id=1809' >/dev/null
  sleep 2
}
capture() {
  adb exec-out screencap -p > "$prefix-$1$suffix.png"
  sips -Z 900 "$prefix-$1$suffix.png" --out "/tmp/tm-r3-$1$suffix.png" >/dev/null
  echo "Captured $1$suffix"
}
crop() {
  adb exec-out screencap -p > "$prefix-title-$1-$4.png"
  sips -c 240 1080 --cropOffset 90 0 "$prefix-title-$1-$4.png" --out "$prefix-title-$1-$4-crop.png" >/dev/null
  sips -Z 900 "$prefix-title-$1-$4-crop.png" --out "/tmp/tm-r3-title-$1-$4.png" >/dev/null
}
if [ "$mode" = screens ]; then
  home; capture home
  tapid browseButton; capture browse
  home; tapid searchButton; capture search
  home; tapid settingsMenuItem; capture settings
  home; lost; capture summary
  tapid tracks; tapid allPartsButton; sleep 4; capture tracks
  tapid summary; tapid sheetMusicLink; sleep 2; capture sheet
elif [ "$mode" = large ]; then
  home; capture home
  lost; capture summary
elif [ "$mode" = titles ]; then
  home; crop home x x "$4"
  tapid browseButton; crop browse x x "$4"
  home; tapid settingsMenuItem; crop settings x x "$4"
  home; lost; tapid sheetMusicLink; crop sheet x x "$4"
fi
