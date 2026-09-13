# Shared vector logo and animated loading copy

The user requested the loader match the background/logo's proportions and angle, then proposed sharing vector artwork for availability and performance.

## Implementation

- Android's existing `ic_barberpole.xml` remains the canonical source. `BarberPoleLogo.kt` derives and caches native paths per indicator, rather than parsing on animation frames. The static background keeps using the existing vector resource.
- iOS replaces its runtime PNG background with `TMLogoBackgroundView`, backed by cached immutable paths from `TMLogoArtwork`. Both the background and animated loader consume this provider.
- All nine contours and 122 cubics come from the canonical artwork. An offline standard-library generator verifies the native paths and a preserved-vector PDF for the iOS launch screen. No runtime SVG dependency or required build-time generation was added.
- The historical iOS PNG is now test-only. Its original opacity, canvas margins and apparent scale are retained in the vector background.
- Both small loaders retain the logo's diagonal silhouette, broad shaft, round finials, collars and highlights. Only clipped red/white/blue bands animate along the shaft. Reduced Motion remains static; the actual background has no animation.

## Evidence

- Android: all five final loader unit tests pass; debug and instrumentation APKs build. Three native query fixture runs pass in light, dark and reduced motion. Phase/pixel assertions confirm static caps/silhouette, movement inside the shaft and seamless loop endpoints.
- iOS: geometry, shared-path caching, static watermark and query lifecycle tests pass on phone/iPad. Background pixels stay unchanged as loader phase advances. Four distinct methods pass across seven device/method combinations for the shared-vector follow-up.
- Historical iOS PNG vs vector overlap is about 96.9%, with artwork bounds within 1px at tested sizes and preserved RGBA opacity. The vector is intentionally not claimed pixel-identical to the raster.
- Canonical generator `--check`, Xcode project plist validation, source registration, removal of production PNG lookup and whitespace checks pass. Native comparison sheets were inspected.
- User app data and device settings restored by workers; no shared-library, signing or account-contract changes.

## Performance scope

The benefit is reusable, scalable artwork and removing runtime watermark PNG decoding. Cached paths and static layers avoid parsing/rebuilding the background during loading animation. No physical-device speedup, frame-rate or package-size improvement is claimed; the asset compiler may retain a raster fallback for the launch PDF.

Reports: `logo-barberpole-android.md`, `logo-barberpole-ios.md`, `shared-vector-artwork-ios.md`. Native evidence uses `tagmaster-*-logo-pole-*.jpg` and `tagmaster-ios-shared-vector-*.jpg`.
