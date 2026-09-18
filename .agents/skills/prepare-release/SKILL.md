---
name: prepare-release
description: Prepare a Pitch Perfect or Tag Master store release in this repository. Review app-specific changes, refresh listing copy and live screenshots, choose independent iOS and Android versions, validate, and open a release PR that deploys the selected app and platforms when merged.
---

# Prepare a mobile release

Read [docs/releases.md](../../../docs/releases.md) for commands, prerequisites, artifact locations, and retry behavior. Work from a clean branch or worktree based on current `origin/main`.

## Select the release

Resolve the app, `pitchperfect` or `tagmaster`, and the requested platforms from the user's request. Ask only when the choice is missing. If both apps are requested, prepare separate plans in the same PR or separate PRs as requested. Never infer that releasing one app releases the other.

Versions are independent for every app/platform pair. Read `scripts/release/apps.json`, existing `releases/<app>/release.json`, and tags matching `<app>/<platform>/v*`. Confirm the current store version and build when store access is available. Suggest the next patch for each selected platform unless the changes or user call for another version. Android's version is not the iOS version. Use a build number greater than the last uploaded build, including unpublished uploads.

For the first release, identify the last shipped commit for each platform from repository and store history. The helper requires an explicit `--since` until a platform has a release tag. Do not use an arbitrary recent commit and present that as the shipped baseline. If baselines differ, pass `--ios-since` and `--android-since` to preserve each baseline.

For an iOS release, inspect the signing repository's profile filenames. If the production profile or Pitch Perfect widget profile is missing, run `release-signing.yml` for the selected app and verify it succeeds before opening the release PR. This provisions signing only. If store permissions or signing-repository write access are missing, report the exact prerequisite; do not merge a release known to be unbuildable.

## Review changes and copy

Inspect the diff and user-facing changes since each selected platform's last release tag. Include the app paths and shared library paths listed in `scripts/release/release.py`. Exclude the other app's changes unless they affect shared functionality. Verify feature claims in the implementation and running app.

Edit `store/<app>/listing.json` when features or wording need updating. Keep platform-specific claims in that platform's copy. Write concrete release notes of at most 500 characters in a temporary text file. Do not publish raw commit messages, internal implementation notes, invented features, or placeholder text. `changes.md` is commit evidence for the PR, not customer-facing notes.

Run `scripts/release/release.py prepare` with the selected versions and notes. It writes only that app's plan and change evidence. Review those files before committing. Editing listing copy alone does not schedule a release.

## Generate and review screenshots

First download and inspect the current screenshots for the selected app and platform with `scripts/release/review.py --download-current`, using a new, separate baseline directory outside the capture bundle. Baseline downloads and captures both require a fresh output directory. After capture, run the review command on the completed bundle without `--download-current` to add a gallery of the new captures. Store baseline images must stay outside Actions bundles. Compare by user flow, populated state, appearance, and device family, not just image count. The public store baseline is the starting point. Preserve coverage unless a feature was removed or there is an explicit reason to replace a scene. Record additions, replacements, and omissions in the PR.

Generate new screenshots for every selected platform from the running native app. Use `scripts/release/capture.py` locally or the `Generate release assets` workflow on the preparation branch. The release PR also generates captures in Actions. iOS requires the documented simulators; Android requires a disposable emulator. Never use a personal device or a simulator containing user data.

Open the generated `index.html`, compare contact sheets with the separate baseline gallery, then inspect every captured PNG at full size. `store_scenes` in `apps.json` selects and orders uploads within the store limits; other captures remain in `review/`. Pitch Perfect includes populated songs, list editing, the song editor, and both Wear OS shapes on Android. Tag Master includes populated favorites, classic browsing, search filters/results, details, tracks, and videos. Both apps capture light and dark appearances on phone and tablet. Example songs and favorites must stay on the disposable device without signing into a real account.

Inspect every captured PNG at full size. Confirm the intended scene has loaded, text is readable, navigation and media controls are visible, and no alerts, empty loading screens, test ads, or personal information appear. Tag Master captures use the live catalog; network failures must be fixed before proceeding. Update capture tests and scene definitions when product flows change. Do not substitute mock screens, image-generated UI, or stale screenshots.

Generated media stays out of git. Commit the plan, listing copy, change evidence, and any intentional capture-test updates. Run the validation and tests in the release guide. Open the PR with `gh pr create --body-file <file>`, then inspect Actions results and download the artifacts for visual review. Fix failures and refresh artifacts if the source changes. Include asset run links and any unresolved store prerequisite in the PR description.

## Handoff

Opening a release PR is the default stopping point. State explicitly that merging it submits the selected versions and assets to App Store review and/or Google Play production. Apple publishes after approval. Do not merge the PR unless the user has authorized merging; an earlier authorization remains valid.

When a merge is authorized, merge only after the release checks and screenshot review pass, then watch `Release mobile apps` through submission. Report app/platform versions, PR, workflow, and release-record URLs. An accepted upload is not proof of store approval or public availability.

For partial failures, follow the retry section in the release guide. Keep the existing version and build when retrying the same commit; do not regenerate numbers just to bypass a failure. Do not rewrite published tags. Stop retrying when the failure needs a store account decision or corrected credentials, and report the exact failed step.
