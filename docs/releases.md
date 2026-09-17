# Mobile releases

Pitch Perfect and Tag Master each have their own listing copy, release plan, screenshots, and release tags. Their iOS and Android versions also change independently. The initial source versions are Pitch Perfect iOS 2.0.3 / Android 4.0.0 and Tag Master iOS 2.0.2 / Android 5.2.1.

Use the repository's `prepare-release` skill to review changes, refresh copy, capture screens, and open a PR. A release is scheduled by changing `releases/<app>/release.json`. Merging that PR into `main` submits only the app/platform pairs in the changed plan. This infrastructure change includes no release plans and does not deploy anything.

## Files and commands

- `store/<app>/listing.json` contains reviewed English store copy. iOS and Android have separate fields and character limits.
- `scripts/release/apps.json` maps app IDs, modules, capture scenes, and initial source versions.
- `releases/<app>/release.json` records the selected platforms, their versions and builds, notes, and source baselines.
- `releases/<app>/changes.md` holds app and shared-library commit evidence for the release PR.
- `build/release/` holds generated files and is ignored by git.

Prepare a release, using your chosen versions and a file containing reviewed notes:

```sh
python3 scripts/release/release.py prepare --app tagmaster \
  --ios-version 2.0.3 --android-version 5.2.2 \
  --since <last-shipped-commit> --notes /tmp/tagmaster-notes.txt
python3 scripts/release/release.py validate
python3 scripts/release/release.py plan --base origin/main
```

Omit a platform's version flag to leave it out of the release. `--since` is required for a platform's first release; subsequent preparations use that platform's latest version tag. If the first iOS and Android releases have different baselines, use `--ios-since` and `--android-since`. Baselines must be ancestors of the preparation commit. The shared paths include PitchPerfectLib, depolllib, Android common libraries, and cloud services.

Build numbers default to Unix seconds and can be overridden with `--build`. Confirm they exceed the latest uploaded build, including builds that have not shipped. The helper enforces increasing versions/builds against the previous plan and published tags. Store APIs remain the authority for uploads made outside this system. Production build overrides apply only to the selected Gradle module or Xcode scheme, including Pitch Perfect's iOS widget. Existing private preview numbering is unchanged. The Android Wear companion is not shipped by these lanes because the Play app has no Wear track configured.

## Generate assets locally

Use Python 3.10 or later, Xcode with the iOS 26 simulator runtime, and JDK 17 plus the Android SDK used by CI. The capture script creates and deletes its own iOS simulators: iPhone 17 Pro Max and iPad Pro 13-inch M5. Both device types must be installed. The script uses `DEVELOPER_DIR` or the runner’s selected Xcode and picks its newest compatible iOS 26 runtime. It never probes other Xcode installations because switching CoreSimulator versions can disrupt other jobs sharing that user’s simulator service. Actions uses the existing self-hosted macOS `heavy` pool and creates disposable simulators independently for each app. On a custom machine, install the selected Xcode's simulator platform with `xcodebuild -downloadPlatform iOS` if needed; `simctl` listing an older runtime alone does not guarantee that Xcode can run tests. Capture retries a stalled simulator boot once, with a three-minute limit per attempt. Android captures use a disposable emulator with the production package ID and clear its app data. Do not point the script at your everyday emulator.

```sh
python3 -m venv .venv-release
.venv-release/bin/pip install -r scripts/release/requirements.txt
git submodule update --init --recursive
.venv-release/bin/python scripts/release/capture.py --app tagmaster \
  --platform ios --output build/release/tagmaster-ios
.venv-release/bin/python scripts/release/capture.py --app tagmaster \
  --platform android --serial emulator-5554 --output build/release/tagmaster-android
```

Android capture uses SystemUI demo mode to hide notification icons and show a consistent 9:41 clock, full battery, and Wi-Fi signal. It reapplies these settings after device size and theme changes, then exits demo mode when capture finishes. The status bar is rendered by Android during capture; screenshots are not retouched.

Actions runs each Android capture through `scripts/release/emulator.py`. The launcher creates a temporary AVD, waits up to five minutes for boot, and stops only its own emulator and capture process groups. Shutdown has a deadline and force-stops stuck children, including children holding output handles open. Failed captures upload emulator logs, logcat, and a diagnostic screenshot separately from the store assets. Tag Master's initial live tag request can use the app's Retry button twice after failures, within the existing two-minute capture deadline.

Choose a fresh output directory for each capture. Android emits phone and 10-inch tablet sets. iOS emits 6.9-inch phone and 13-inch iPad sets. Both appearances are captured for every scene:

| App | Captured flows per appearance |
| --- | --- |
| Pitch Perfect | Pitch pipe, notes, keys, populated songs, editing/reordering, song editor |
| Tag Master | Populated favorites, classic browsing, search filters, search results, summary, details, loaded learning tracks, videos |

Each iOS job owns a separate simulator and runs concurrently with other jobs within the runner pool's existing capacity. Preview archives also use the heavy pool so they cannot add an unbounded third build beside simulator jobs. Xcode builds use two compiler jobs to limit peak memory. A short allocation lock protects device creation and ownership records; builds, boot, tests, and captures do not hold it. Records identify the Actions job's `Runner.Worker` process and its start time, so ownership survives shell-step boundaries and PID reuse cannot preserve abandoned devices. Cleanup removes disposable devices from exited jobs and skips live owners. Cancellation stops only the command's process group and simulator. Shutdown and deletion each have a 30-second limit; a stuck shutdown still attempts deletion. Test simulators explicitly finish boot before XCTest starts. For one-time cleanup of a known legacy shared CI device, set the repository variable `IOS_LEGACY_SIMULATOR_UDID` to its exact UDID. CI shuts down only that device if it has no live owner, preserving its data. Clear the variable after the retirement step succeeds.

The regular iOS CI job runs each unit-test target before the UI regressions and interrupts XCTest after the first failed case. It enforces a 30-second duration budget per regression test, saves unfiltered logs, and reports the slowest cases. After simulator boot, a prebuilt test command has five minutes to start its first case; startup progress is reported every 30 seconds. A failed or hung command gets 15 seconds to stop before its owned process group is killed. Keep each UI test focused on one screen or interaction, check whether controls are already ready before waiting, and wait for the state the assertion needs, such as tappability after rotation. Slow test cases and stalled startup save process stacks and resource snapshots in the test-results artifact. Both UI-test schemes use automatic screenshots instead of screen recording, avoiding XCTest video-finalization stalls during teardown. Failure screenshots and explicit store screenshot attachments remain available. Store captures dismiss SpringBoard notification banners before taking a screenshot and recheck afterward, so first-boot system announcements cannot cover the listing images. The opt-in `StoreScreenshotNotificationTests` case exercises this with a real local notification; run it with `TEST_RUNNER_STORE_NOTIFICATION_TEST=1` and the `pitchperfectUITests` scheme. Store screenshot tours run separately as asset-generation jobs.

The tests use the real native views. Example songs are local to the disposable device; Tag Master's favorites and results load from the live catalog. No account sign-in or remote user-data writes are needed. Missing/deleted catalog entries, empty results, and failed media loads fail capture. Pitch Perfect captures its existing ad-free state on Android. Its iOS debug build suppresses ad requests; production builds have no capture switch.

`apps.json` separates the complete capture scenes from the selected and ordered `store_scenes`: up to ten uploads per iOS device size and eight per Play phone/tablet category. Extra light/dark scenes remain in `review/` for visual review, outside the upload directories. Do not discard existing coverage merely to keep the automation short.

Pitch Perfect Android also requires round and square Wear OS captures. Boot disposable API 33 Wear OS emulators with `wearos_small_round` and `wearos_square` profiles. Ensure the round AVD has `hw.lcd.circular=true` before booting. Capture each, then pass their directory to the phone/tablet command:

```sh
.venv-release/bin/python scripts/release/wear.py --serial emulator-5556 \
  --shape round --output build/release/wear
.venv-release/bin/python scripts/release/wear.py --serial emulator-5558 \
  --shape square --output build/release/wear
.venv-release/bin/python scripts/release/capture.py --app pitchperfect \
  --platform android --serial emulator-5554 --wear-source build/release/wear \
  --output build/release/pitchperfect-android
```

Each watch capture holds the C4 cell so the instrument is shown sounding. The watch images must match the current source and actual display shapes. The Play lane uploads them through `wearScreenshots`; it still does not release a Wear binary. Actions creates and captures the watch emulators automatically and deletes each before starting the next device. Wear OS images can require larger partitions than the requested 2 GB; provision sufficient free disk space on capture runners. The workflow never removes host SDKs.

Download the current public store screenshots and build a comparison gallery for any capture bundle:

```sh
.venv-release/bin/python scripts/release/review.py --app tagmaster \
  --platform ios --output build/release/tagmaster-ios
```

Open `index.html` inside the bundle. It links every full-size image, contact sheets, and downloaded `current-store/` screenshots with source URLs and retrieval time. The reference screenshots are never uploaded. Actions includes this comparison in every artifact and fails if the current listing cannot be retrieved. The public Play page groups multiple device types together; inspect the actual images when mapping coverage.

The validator checks the exact scene set, PNG dimensions, opaque pixels, blank images, duplicate scenes, file hashes, app, platform, and source commit. Inspect the images as well: pixel checks cannot judge copy legibility or whether a remote asset has finished loading. Update the native tests and `apps.json` together when the screens change. The initial baseline comparison found five light/dark flows per iOS device for each app, plus phone/tablet songs and round/square watches for Pitch Perfect Android, and seven distinct Tag Master Android flows. Review these against the current listings on every release.

## Generate in Actions

`Generate release assets` supports a manual app/platform selection and can run on a preparation branch:

```sh
gh workflow run release-assets.yml --ref <branch> -f app=tagmaster -f platform=ios
gh run list --workflow release-assets.yml
gh run download <run-id> --name release-assets-tagmaster-ios --dir /tmp/tagmaster-ios
```

The workflow must first exist on the default branch for manual dispatch. Release PRs automatically call the same workflow for each selected pair. Tooling or copy PRs without a release plan capture both apps on both platforms for regression coverage. Artifact names contain both app and platform. Each artifact contains screenshots, capture provenance, and release copy when a plan exists. Artifacts expire after 30 days; successful store submissions also archive assets on a GitHub release.

After capture finishes, Actions adds or updates a PR comment with download links for each app/platform artifact. Failed captures link to the run logs. Extract a ZIP and open `index.html` to review the screenshots. Generated screenshots, comparison images, and galleries stay in Actions artifacts or ignored local output directories; never commit them to git.

On merge, `Release mobile apps` regenerates screenshots from the merged commit, then downloads only artifacts from that run. Each production lane validates the capture identity before exporting copy and building its binary. No rolling `latest` asset bundle is shared between apps. Apple uploads replace screenshots for the captured device classes; Play uploads include copy, screenshots, release notes, the app bundle, and for Pitch Perfect the listing icon, which `scripts/release/icons.py` derives from the launcher icon at capture time so the two never drift. Feature graphics and Tag Master's icon remain managed in the stores.

## Store access and first-use setup

The workflows reuse the preview credentials:

- Android: `ANDROID_UPLOAD_KEYSTORE`, `ANDROID_UPLOAD_KEYSTORE_PASSWORD`, `ANDROID_UPLOAD_KEY_ALIAS`, `ANDROID_UPLOAD_KEY_PASSWORD`, and `PLAY_SERVICE_ACCOUNT_JSON`. The service account accepts raw or base64 JSON and needs production release/listing permissions for both packages.
- iOS: `APP_STORE_CONNECT_API_KEY_ID`, `APP_STORE_CONNECT_API_ISSUER_ID`, `APP_STORE_CONNECT_API_KEY_CONTENT`, `MATCH_PASSWORD`, and `MATCH_GIT_SSH_KEY`. `MATCH_GIT_BASIC_AUTHORIZATION` is the HTTPS fallback.

The private signing repository must contain App Store profiles for `depollsoft.tagmaster`, `depollsoft.pitchperfect`, and `depollsoft.pitchperfect.widget`, with their production entitlements. Deployment lanes use readonly Match. Before the first iOS release, run the one-time provisioning workflow if a profile is missing:

```sh
gh workflow run release-signing.yml -f app=tagmaster
```

`Provision production signing` runs only from `main`, creates or repairs profiles for the selected app, and stores them in the encrypted signing repository. It requires a signing-repository credential with write access. It does not upload a binary or release an app. During implementation, the remote signing repository had Pitch Perfect's production app/widget profiles but lacked Tag Master's production profile, so run this for Tag Master after the tooling PR merges.

 The iOS API key needs access to both production apps. The existing store records must have their review contact, content rights, age rating, privacy, encryption, and advertising answers completed. This tooling does not invent those answers. Store agreements or new required fields can block submission and need correction in the console.

Release jobs use the existing self-hosted pools, so they do not require paid hosted-runner minutes. All native captures use macOS runners labeled `heavy` because hosted Ubuntu cannot reach Tag Master’s live catalog and the Linux pool has no usable KVM device. A catalog preflight checks connectivity before building. Captures use temporary emulators, with port 5560 reserved for Tag Master and 5554 for Pitch Perfect, and serialize each app/platform across runs. Each Android capture owns a separate ADB server on an available private port, with emulator auto-discovery disabled. A host lock serializes the complete emulator lifetime, from boot through shutdown, across phones, tablets, and watches. This also protects host services shared by emulator processes on different runner slots. A failed iOS tour resets only its disposable simulator before its one retry. The workflow never changes host KVM permissions or removes SDKs. Fork PRs do not execute release jobs on self-hosted runners. No deployment credentials are passed to capture jobs. Plan validation, PR artifact links, and Android deployment use Linux; iOS deployment uses macOS. Consider making the release checks required in branch protection.

## Submission and retries

Tag Master's screenshots depend on the live catalog and media. Each device/theme capture gets one retry after 15 seconds, with fresh app data and the same build. Only a fully successful native test supplies screenshots. A second failure stops the job; asset validation and source checks are never retried or bypassed.

Apple submission requests automatic release after review. Google Play receives a completed production release with changes sent for review. Neither means the store has already approved or published the app.

After each platform succeeds, the workflow creates a release record such as `tagmaster/ios/v2.0.3` or `pitchperfect/android/v4.0.1`, with its asset archive, notes, and source commit. Tags are app/platform scoped and cannot be reused for different source commits. A successful platform does not wait for another platform's store submission to succeed.

For a transient failure, use **Re-run failed jobs** on the original workflow. Its source, version, build, and artifacts remain fixed. A full rerun also accepts already-published tags that point to that same commit. iOS reuses an uploaded build number after an interrupted submission. If the exact version/build is already in review, approved, or released, it skips submission. Play checks release lifecycle states and also skips the exact production build while it is in review, approved, or published. Draft, rejected, and unsent Play releases require attention instead of being recorded as submitted. A published GitHub release with an asset archive marks completion. If publication of that record was interrupted, rerunning completes it. Do not change version numbers or force-move tags to retry.

If the source or copy needs correcting after a partial release, prepare a new release plan for only the affected platform with a new version/build. If a later release already shipped, do not rerun an older deployment.

## Checks

```sh
.venv-release/bin/python -m unittest discover -s scripts/release/tests -v
python3 scripts/release/release.py validate
ruby -c scripts/release/production.rb
ruby scripts/release/tests/test_production.rb
actionlint .github/workflows/release*.yml
```

The unit tests cover isolated app selection, platform versions, initial infrastructure merges, version/build regressions, retries, copy export, and corrupt or incomplete screenshots. Live capture builds and runs the native tests. Signed submission requires store credentials and is exercised by a release PR, not an infrastructure PR.

References: [Fastlane App Store delivery](https://docs.fastlane.tools/actions/deliver/), [Fastlane Play delivery](https://docs.fastlane.tools/actions/upload_to_play_store/), and the apps' existing [support](https://apps.depoll.com/) and [privacy](https://apps.depoll.com/privacy/) pages.
