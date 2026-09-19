#!/bin/sh
# Xcode's default checkout and the custom caches used by preview/release builds.
set -eu

case "${PLATFORM_NAME:-}" in
    *simulator*)
        echo "Skipping Crashlytics symbol upload for simulator build."
        exit 0
        ;;
esac

for packages_dir in \
    "${BUILD_DIR%/Build/*}/SourcePackages" \
    "${SRCROOT}/../SourcePackages" \
    "${SRCROOT}/../../build/release/SourcePackages"
do
    crashlytics_run="${packages_dir}/checkouts/firebase-ios-sdk/Crashlytics/run"
    if [ -f "$crashlytics_run" ]; then
        exec /bin/sh "$crashlytics_run"
    fi
done

echo "error: Firebase Crashlytics upload script was not found. Resolve Swift packages before building." >&2
exit 1
