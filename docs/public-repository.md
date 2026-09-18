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

## Public pull requests and private runners

External PR builds remain disabled in the mobile and release workflows.
The preview metadata job also requires a same-repository PR and runs on a
GitHub-hosted runner. The comment-command dispatcher uses a hosted runner, and
the self-hosted smoke test is manual only.

These workflow conditions do not replace runner isolation: PR authors can
propose changes to workflow files. Before publication, prevent the public repo
from scheduling jobs on persistent private runners, for example by moving those
runners and trusted deployment workflows to a private repo with restricted
access. Alternatively, use isolated disposable runners. The code changes alone
do not configure that infrastructure boundary.
