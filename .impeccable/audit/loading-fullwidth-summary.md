# Full-width tabs and quartet loading

User requested full-width bottom tabs and a more delightful initial loading state, with changes on both platforms where appropriate.

## Delivered

- Android Browse and Detail: four equal slots span the safe available width. Removed the content-width cap, automatic sizing and per-tab maximum that previously left gaps. Large labels wrap and grow the strip instead of forcing scrolling.
- iOS Browse and Detail: app-local native buttons replace UIKit's narrow floating page group. Four equal slots fill the safe width, including the iPad split detail column. Page containment, labels, icons, selected traits and navigation remain intact.
- Both platforms: four gently moving blue notes on a short staff, "Gathering the quartet…", and a truthful loading label with the tag ID. Unpopulated fields/actions are hidden, Back stays available, and content replaces the loader immediately on completion.
- Existing tags stay visible during refresh and after refresh failure. Errors offer Retry. Stale completions cannot overwrite another tag.
- No fake progress, artificial delays, sound or haptics. Reduced-motion users see a static staff. Animation stops when hidden, detached or backgrounded.

## Verification

- Android: 20 targeted unit tests and 61 unique native tests pass, plus motion-enabled lifecycle and capture variants. Debug and instrumentation APKs build. Equal slot geometry checked at phone, landscape, expanded and font scale 2.0.
- iOS: 35 distinct focused unit tests have passing results across the recorded runs. Three real-app journeys pass on each phone/iPad, including full-width taps and rotation. The final coordinator run passes 4/4 contrast, width, containment and delayed-load checks. No width-test exemption remains.
- Native loading/loaded snapshots reviewed in light, dark, large text, landscape and expanded layouts. iPad real-app split-view captures confirm sidebar clearance.
- Device settings and private app data restored by platform workers. No shared-library, signing, privacy or sync-contract changes. Compiler/test registration and whitespace checks pass.
- Not claimed: full repository test-suite pass for this change, physical-device performance or manual VoiceOver/TalkBack speech testing.

Detailed evidence: `loading-android.md`, `loading-ios.md`. Captures in `.impeccable/review/tagmaster-*-loading-*.jpg` and `tagmaster-ios-fullwidth-*.jpg`.
