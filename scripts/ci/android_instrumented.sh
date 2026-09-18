#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../Android"

# These fixtures install JVM-wide URL handlers that cannot be removed. Give
# each its own instrumentation process and keep them out of the shared suite.
isolated=(
  depollsoft.tagmaster.BarberPoleQueryRegressionTest
  depollsoft.tagmaster.CompactLoadingRegressionTest
)
excluded=$(IFS=,; echo "${isolated[*]}")
archive=$(mktemp -d)
trap 'rm -rf "$archive"' EXIT
result=0
options=(--no-parallel --build-cache --stacktrace -Pandroid.testInstrumentationRunnerArguments.coverage=true)

for fixture in "${isolated[@]}"; do
  if ! ./gradlew :TagMaster:connectedDebugAndroidTest "${options[@]}" \
    "-Pandroid.testInstrumentationRunnerArguments.class=$fixture"; then
    result=1
  fi
  for kind in androidTest-results code_coverage; do
    source="TagMaster/build/outputs/$kind"
    if [ -d "$source" ]; then
      mkdir -p "$archive/$fixture/$kind"
      cp -R "$source/." "$archive/$fixture/$kind/"
    fi
  done
done

if ! ./gradlew connectedDebugAndroidTest "${options[@]}" \
  "-Pandroid.testInstrumentationRunnerArguments.notClass=$excluded"; then
  result=1
fi

# Retain XML and execution data from every invocation for the CI publishers.
for fixture in "${isolated[@]}"; do
  for kind in androidTest-results code_coverage; do
    if [ -d "$archive/$fixture/$kind" ]; then
      destination="TagMaster/build/outputs/$kind/connected/isolated/$fixture"
      if [ "$kind" = code_coverage ]; then
        destination="TagMaster/build/outputs/$kind/debugAndroidTest/connected/isolated-$fixture"
        mkdir -p "$destination"
        python3 - "$archive/$fixture/$kind" "$destination" <<'PY'
from pathlib import Path
import shutil
import sys
for index, source in enumerate(sorted(Path(sys.argv[1]).rglob('*.ec'))):
    shutil.copyfile(source, Path(sys.argv[2]) / f'{index}.ec')
PY
        continue
      fi
      mkdir -p "$destination"
      cp -R "$archive/$fixture/$kind/." "$destination/"
    fi
  done
done
if ! ./gradlew createDebugAndroidTestCoverageReport "${options[@]}" -x connectedDebugAndroidTest; then
  result=1
fi
exit "$result"
