# Product

<!-- impeccable:product-schema 1 -->

This record covers Pitch Perfect and Tag Master, the two DepollSoft music apps in this repository.

## Platform

adaptive

Both products support native iOS and Android implementations. Product behavior and terminology should remain consistent while each app follows its platform's interaction conventions.

## Users

Pitch Perfect serves singers and musicians who need a fast reference pitch and a reliable way to recall song keys.

Tag Master serves barbershop singers and teachers who browse tags, learn voice parts, organize material, and use the app while singing with other people. Social singing at afterglows and similar gatherings is a primary use case, not a secondary extension of study.

## Product Purpose

Pitch Perfect replaces a physical pitch pipe with a chromatic digital pitch pipe and keeps useful musical references close at hand. Users can play notes, inspect keys, and save songs with their key signatures.

Tag Master helps people find, prepare, teach, and sing barbershop tags. It brings catalog details, sheet music, learning tracks, videos, favorites, teachable lists, and sharing into a mobile workflow suited to both individual preparation and group singing.

Success means a user can reach the right note, song key, tag, chart, or learning track quickly enough that the app supports the musical moment rather than interrupting it.

## Positioning

Pitch Perfect combines a practical chromatic pitch pipe with song and key-signature recall. It supports immediate pitch finding as well as a musician's saved repertoire.

Tag Master turns the BarbershopTags.com catalog into an active singing companion. Search and study matter, but the product also supports the live social ritual of choosing and singing tags together at afterglows and other gatherings.

## Operating Context

Pitch Perfect is used during rehearsal, preparation, performance setup, and other moments when a singer or musician needs a starting pitch or key reference. The app may be used briefly and one-handed, so the core pitch action must remain immediate.

Tag Master is used alone for browsing and practice, by teachers preparing material, and in groups who are actively choosing and singing tags. Users may search by title or lyrics, filter by parts and available materials, open a tag by ID, play individual voice parts, view or share sheet music, watch teaching videos, and move between prepared lists during a social gathering.

Accounts are optional. Local use remains possible, while signed-in users can back up and synchronize saved data across devices.

## Capabilities and Constraints

- Preserve native iOS and Android support for both products.
- Preserve the names Pitch Perfect and Tag Master.
- Preserve established music terminology, including pitch pipe, notes, keys, songs, key signatures, tags, learning tracks, parts, tenor, lead, baritone, bass, favorites, and teachable tags.
- Pitch Perfect supports chromatic note playback, key references, saved songs, key signatures, themes, and optional account-based synchronization of songs and settings. Android also contains widget and Wear integrations.
- Tag Master supports BarbershopTags.com browsing and search, tag details, random selection, ratings, sheet music, learning tracks, videos, part playback, favorites, teachable lists, sharing, caching, and optional account-based synchronization of tag lists.
- Keep the BarbershopTags.com relationship and content attribution intact.
- Preserve current cloud-sync and privacy commitments. Do not broaden claims about personal-data collection without verified product and legal review.
- Current code spans Swift, Objective-C, Java, Kotlin, Firebase, and shared backend services. Modernization must preserve working data, account, deep-link, and content contracts.

## Brand Commitments

Pitch Perfect, Tag Master, DepollSoft, and BarbershopTags.com are established names in the product experience. The products use specific music-community language rather than generic media or education terminology.

Pitch Perfect keeps its long-standing grayscale-first aesthetic: neutral monochrome surfaces carry the interface, with at most a single luminous emphasis for the sounding note. Saturated multi-color themes are off-brand for this app.

Tag Master must respect barbershop culture as a participatory social practice. Copy and product decisions should recognize singers, teachers, voice parts, tags, and afterglows without flattening them into generic lesson content.

## Evidence on Hand

- Product code and user-facing copy for Pitch Perfect in `Android/PitchPerfect/` and `iOS/pitchperfect/`.
- Product code and user-facing copy for Tag Master in `Android/TagMaster/` and `iOS/tagmaster/`.
- App icons and shipped image assets in both native projects.
- Unit and UI tests for major Pitch Perfect and Tag Master workflows under the Android and iOS test targets.
- Firebase projects and functions for each app under `Firebase/pitchperfect/` and `Firebase/tagmaster/`.
- Shared API and analytics infrastructure under `api/` and the native shared libraries.
- Existing copy documents account sync, deletion, content attribution, terms links, and Pitch Perfect's personal-data claim.

No verified testimonials, press quotes, case studies, audience totals, or current performance benchmarks were identified during init. Future work must not fabricate them.

## Product Principles

1. Keep the musical action immediate. A pitch, key, tag, chart, or part should take as few decisions as the task allows.
2. Design for live social use as well as private study. Tag Master must work when people are together and ready to sing.
3. Use the community's language accurately. Domain terms carry meaning and should not be replaced with generic labels.
4. Preserve user continuity. Saved songs, tag lists, preferences, links, and account data must survive platform and interface changes.
5. Adapt to each native platform without splitting product behavior or terminology.
