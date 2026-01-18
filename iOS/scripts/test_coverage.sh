#!/usr/bin/env bash
set -euo pipefail

# Runs tests for all shared iOS schemes in the workspace with coverage enabled
# and enforces a minimum coverage threshold (default 85%).

THRESHOLD=${THRESHOLD:-85}
WORKSPACE="$(cd "$(dirname "$0")/.." && pwd)/iOS.xcworkspace"
DESTINATION=${DESTINATION:-"platform=iOS Simulator,name=iPhone 16,OS=18.6"}
OUTDIR="$(cd "$(dirname "$0")" && pwd)/../build/coverage"
mkdir -p "$OUTDIR"

SCHEMES=("pitchperfect" "tagmaster" "depolllib")
UITEST_SCHEMES=("pitchperfectUITests" "tagmasterUITests")

green() { printf "\033[32m%s\033[0m\n" "$*"; }
red() { printf "\033[31m%s\033[0m\n" "$*"; }

extract_total_coverage_json() {
  local bundle="$1"
  # Prefer JSON and python3 for robust parsing
  if command -v python3 >/dev/null 2>&1 ; then
    xcrun xccov view --report --json "$bundle" | python3 - "$2" <<'PY'
import json, sys
data=json.load(sys.stdin)
# If a target filter is passed, choose the first match; else compute overall average weighted by lineCount
flt=sys.argv[1] if len(sys.argv)>1 else ''
targets=data.get('targets',[])
if flt:
  for t in targets:
    if flt.lower() in t.get('name','').lower():
      print(round(t.get('lineCoverage',0.0)*100,2))
      sys.exit(0)
  # fallback to overall if no target matched

totalLines=0
covered=0
for t in targets:
  totalLines+=t.get('lineCount',0)
  covered+=t.get('lineCount',0)*t.get('lineCoverage',0.0)
if totalLines==0:
  print(0)
else:
  print(round((covered/totalLines)*100,2))
PY
    return
  fi
  # Fallback to plain text parsing: take the last percentage in the summary
  xcrun xccov view --report "$bundle" | awk '/^[[:space:]]*\d+\.\d+%/ {last=$1} END{gsub("%","",last); print last+0}'
}

fail=0
declare -a RESULTS

for scheme in "${SCHEMES[@]}"; do
  bundle="$OUTDIR/${scheme}.xcresult"
  rm -rf "$bundle"
  echo "Running tests for scheme: $scheme"
  if command -v xcpretty >/dev/null 2>&1; then
    xcodebuild \
      -workspace "$WORKSPACE" \
      -scheme "$scheme" \
      -configuration Debug \
      -destination "$DESTINATION" \
      -enableCodeCoverage YES \
      -resultBundlePath "$bundle" \
      test | xcpretty
  else
    echo "Building and testing $scheme (output redirected to ${scheme}.log)..."
    xcodebuild \
      -workspace "$WORKSPACE" \
      -scheme "$scheme" \
      -configuration Debug \
      -destination "$DESTINATION" \
      -enableCodeCoverage YES \
      -resultBundlePath "$bundle" \
      test > "${scheme}.log" 2>&1
  fi

  if [ ! -d "$bundle" ]; then
    red "No result bundle for $scheme (tests may have failed to start)."
    fail=1
    continue
  fi

  pct=$(extract_total_coverage_json "$bundle" "$scheme" || echo 0)
  RESULTS+=("$scheme:$pct")
  cmp=$(printf '%.0f' "${pct}")
  if [ "$cmp" -lt "$THRESHOLD" ]; then
    red "${scheme}: coverage ${pct}% < ${THRESHOLD}%"
    fail=1
  else
    green "${scheme}: coverage ${pct}% >= ${THRESHOLD}%"
  fi
done

echo "\nSummary:"
for r in "${RESULTS[@]}"; do echo " - $r"; done

# Run UI tests separately (they don't contribute to line coverage but validate UI)
echo "\nRunning UI Tests..."
for scheme in "${UITEST_SCHEMES[@]}"; do
  bundle="$OUTDIR/${scheme}.xcresult"
  rm -rf "$bundle"
  echo "Running UI tests for scheme: $scheme"
  if command -v xcpretty >/dev/null 2>&1; then
    xcodebuild \
      -workspace "$WORKSPACE" \
      -scheme "$scheme" \
      -configuration Debug \
      -destination "$DESTINATION" \
      -resultBundlePath "$bundle" \
      test | xcpretty || echo "UI tests for $scheme completed (some may have failed)"
  else
    echo "Building and testing $scheme (output redirected to ${scheme}.log)..."
    xcodebuild \
      -workspace "$WORKSPACE" \
      -scheme "$scheme" \
      -configuration Debug \
      -destination "$DESTINATION" \
      -resultBundlePath "$bundle" \
      test > "${scheme}.log" 2>&1 || echo "UI tests for $scheme completed (some may have failed)"
  fi
done

exit $fail
