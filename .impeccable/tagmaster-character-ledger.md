# Tag Master character correction

## User approval

The user reviewed the corrected native screenshots and explicitly requested pushing this correction to the existing PR. This acceptance supersedes the earlier pending-approval marker below. Density and clearer separation of Favorites and Teachable Tags are the next refinement; arbitrary labels/custom lists remain future work, not implemented.

## Binding feedback

The user rejected the first refresh as sterile, missing the barber-pole background, and confusing through nested tabs. The original request to preserve visual character was not satisfied. Earlier technical passes and fix-scope reviews do not approve the rejected design.

## Acceptance

- [ ] Original page-scale barber pole is visibly present behind Home, catalog/results and tag content in both appearances, including populated screens. It is not a footer/icon substitute, hidden layer, or multiplied-opacity ghost.
- [ ] One background per visible workspace, with readable text and no image over the actual chart/video content.
- [ ] Familiar Home -> Browse/Search -> Tag -> material flow. No new global tabs/sidebar and no bottom navigation that changes from app destinations to tag sections.
- [ ] Home exposes Find, Browse, Random, Open ID, Favorites/Teachable and Settings without redundant headings or opaque card/pill scaffolding.
- [ ] Catalog collections use a labeled local selector. Phone material pages push from the tag; tablets use their extra space without nested app navigation.
- [ ] Native Back returns to the actual caller and preserves query/filter/tag context. Large text and narrow windows remain usable.
- [ ] Retain data keys/migrations/sync, media cancellation/retry, chart ordering, accessible rating layout, attribution contrast, empty/error recovery and native share anchors.
- [ ] Targeted route/layout and retained regression checks, actual iOS/Android phone/tablet-size screenshots, one correction batch.
- [ ] Show corrected native screenshots to the user before committing or updating the existing PR. User acceptance is still required.

## Authority

Original implementation `41d73801` and shipped artwork establish visual character. The corrective review is `.impeccable/review/tagmaster-character-correction.md`. The user feedback supersedes the first refresh's quiet-footer and global-navigation rules.

## Scope

Only Tag Master native code/tests and scoped design/evidence records. Pitch Perfect and backend remain unchanged. No further model-driven theme selection or broad redesign. All subagents use Astra.
