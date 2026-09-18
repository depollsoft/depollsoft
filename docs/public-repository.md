# Public repository preparation

Firebase client config files, OAuth client IDs, ad IDs, and the shared
`Android/debug.jks` development key are intentionally public. Production signing
keys and service-account credentials belong in GitHub Actions secrets or the
deployed service's secret store.

## Android signing

Release builds require `ANDROID_UPLOAD_KEYSTORE_PATH`,
`ANDROID_UPLOAD_KEYSTORE_PASSWORD`, `ANDROID_UPLOAD_KEY_ALIAS`, and
`ANDROID_UPLOAD_KEY_PASSWORD`. They no longer fall back to the development key.

Private APKs for sideloading explicitly select development signing:

```sh
cd Android
./gradlew :PitchPerfect:assemblePrivate :TagMaster:assemblePrivate \
  :PitchPerfectWear:assemblePrivate -PDEPOLLSOFT_PRIVATE_DEBUG_SIGNING=true
```

The PR preview workflow supplies this flag. Play upload workflows supply the
upload credentials and leave the flag unset. Normal debug builds retain their
existing signing configuration.

## Preventing new leaks

The `Check repository secrets` workflow runs on GitHub-hosted runners. It checks
the current files and new PR commits, including secrets deleted later in a PR.
Its Gitleaks binary is pinned by version and SHA-256. It accepts Firebase API
keys only in client config paths; private keys and unrelated tokens in those
same files are still checked.

`scripts/ci/check_sensitive_files.py` also rejects tracked user exports, local
environment files, legacy credential files, and non-debug signing material.
Ignore rules prevent accidental additions. Neither ignore rules nor the scanner
can identify every possible export filename or confidential value.

## History cleanup and publication

The removed `Firebase/pitchperfect/allUsers` file contained user identifiers and
must be removed from published history. Historical Parse master-key and Fabric
build-secret material must also be purged. Fabric copies existed in the Android
properties files and the iOS Xcode project build scripts.

A normal cleanup commit does not erase these earlier versions. Before making
the existing repository public:

1. Apply and verify the prepared history rewrite across all published branches
   and tags. Coordinate with collaborators because commit IDs change. Re-clone
   or rebase active work onto the cleaned history; merging old branches can
   restore the removed history.
2. Resolve GitHub's retained pull-request refs and cached views with GitHub
   Support where needed. Those refs cannot be replaced with a normal Git push.
3. Confirm the retired Parse/Fabric credentials have no surviving valid use.
4. Review existing Actions artifacts and logs before their visibility changes.
5. Enable GitHub secret scanning and push protection where available.

## GitHub-hosted runners and public pull requests

All workflows use GitHub-hosted runners: `ubuntu-latest` for Android, API,
uploads, and metadata; `macos-latest` for iOS builds, signing, and screenshots;
and `windows-latest` for the manual Windows smoke check. Android screenshots
use Linux with KVM. Each job gets a fresh machine. Signing and deployment
credentials remain in GitHub Actions secrets.

External PR builds remain disabled in the mobile and release workflows.
The secret scanner still checks external PRs. The runner smoke workflow is
manual only.

Keep public-repository access disabled on the organization's self-hosted runner
groups, or remove this repository from their allowed repositories. Workflow
conditions alone cannot prevent a PR from requesting a self-hosted runner.
No workflow in the default branch requires access to those runners.
