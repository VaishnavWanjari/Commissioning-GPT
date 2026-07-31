# CollageX (Android, Kotlin + Jetpack Compose)

> "Create Instagram-worthy posts and stories in under 30 seconds."

A native Android app (Kotlin, Jetpack Compose, Material 3) that turns a batch of
5–20 photos into a finished, exportable collage/carousel/mood-board without any
manual placement. Built as a standalone module in this repo, same pattern as
`android-app/`.

## What v1 actually does (all real, all on-device, no network calls)

- **Photo picker** — Android system Photo Picker, 1–20 images, no storage
  permission prompt needed.
- **AI Smart Collage** — `collage/CollageLayoutEngine.kt` picks a layout style
  automatically from photo count + aspect-ratio spread
  (`suggestStyle`), then renders one of six real geometric algorithms (grid,
  film strip w/ sprockets, polaroid scatter w/ rotation jitter, magazine
  hero-split, column-balanced masonry, single hero) across 15 named styles
  (Film Strip, Polaroid, Magazine, Scrapbook, Pinterest, Minimal, Luxury,
  Vintage, Summer, Travel Diary, Wedding, Birthday, Couple, Cafe, Beach).
  There is **no trained ML model** behind this — it's deterministic geometry,
  see Roadmap below for the honest gap.
- **One-tap color grading** — 14 presets (Moody, Brown, Warm, Cold, Vintage,
  Film, Kodak, Fuji, Leica, Pastel, Clean Girl, Pinterest, Cinematic,
  Original), each a genuine `android.graphics.ColorMatrix`
  (`collage/ColorGradingPresets.kt`), applied instantly with
  `ColorMatrixColorFilter`.
- **Text + stickers editor** — draggable text (9 style pairings across the 4
  platform type families) and an emoji sticker pack, baked into the final
  export by `collage/OverlayRenderer.kt`.
- **Carousel builder** — `collage/CarouselSplitter.kt` splits the final
  artwork into 3/4/5/6/10 seamless square tiles for an Instagram carousel.
- **Feed preview** — 3×3 / 6×6 grid preview of how the export sits in a feed.
- **Caption + hashtag generator** — offline, rule-based template bank keyed by
  mood (Travel/Birthday/Gym/Cafe/Love/Friends/Graduation),
  `collage/CaptionGenerator.kt`. Not an LLM call — instant and free.
- **Export** — save to gallery (`Pictures/CollageX`), share via the system
  share sheet, or share straight to **Instagram Stories** (Instagram's public
  `com.instagram.share.ADD_TO_STORY` intent — no API key/login, just an
  installed Instagram app, with a safe fallback to the generic share sheet).
- **Freeform canvas** (`collage/FreeformComposer.kt`) — the SCRL-style
  alternative to fixed templates: drag, pinch-scale and rotate every photo
  independently on a solid or gradient background (8 built-in gradients),
  live via `graphicsLayer` transforms, baked to a real bitmap on demand.
- **Collage frames** (`collage/FrameOverlay.kt`) — thin/thick white or rounded
  black border drawn around the whole finished piece, applied after grading.

## Feature parity vs. SCRL (Play Store / App Store)

Researched via web search of SCRL's store listings, review/UI-breakdown sites,
and pricing pages (direct page fetches were blocked by bot-protection in this
sandbox, so this is a synthesis of search snippets, not a scrape). Where
CollageX doesn't yet match, it's called out as a gap, not silently skipped.

| SCRL feature | CollageX v1 |
|---|---|
| Hundreds of hand-crafted collage/carousel/story templates | 15 named styles across 6 real layout algorithms + auto-suggestion |
| Freeform canvas (drag/resize/rotate each photo) | ✅ Freeform mode, pinch/drag/rotate per photo |
| Structured/grid templates | ✅ Structured mode (the original 15 styles) |
| 10+ photos per post | ✅ up to 20 |
| Layering: stack photos/text/stickers, rotate | ✅ (opacity control is not yet exposed — see Roadmap) |
| Gradient backgrounds (premium in SCRL) | ✅ 8 built-in gradients, free |
| Video inside grids (premium in SCRL) | ❌ — no video pipeline this round, see Roadmap |
| Hundreds of custom stickers/overlays, frames | ~30 emoji-based stickers + 4 collage frames (not custom-drawn art) |
| Trendy custom fonts (users note limited variety) | 9 style pairings across 4 platform type families (no bundled display fonts yet) |
| Seamless carousel templates | ✅ 1/3–1/10 carousel splitter |
| Direct post to Instagram/TikTok (no export step) | ✅ Direct Instagram Stories intent; TikTok has no equivalent public "add to story" intent, so it goes through the normal Android share sheet (still one tap, just not a bespoke API) |
| Free core app + Premium subscription (weekly/yearly, video + gradients + full template library gated) | Free-by-default in this v1; no paywall/billing built yet |
| Cut-out / background removal | ❌ — needs an ML segmentation model, see Roadmap |

## Explicitly not in v1 (see Vision doc for full scope)

These were in the original product brief but need infra this session couldn't
responsibly fake:

- Trained-model "AI" (mood detection from pixels, background replacement,
  photo cleanup / magic erase, sky replacement, skin retouch, cut-out) — needs
  a real ONNX/TFLite model + training data.
- Video support inside grids/carousels — needs a video decode/encode pipeline
  (FFmpeg or Media3) neither built nor testable in this sandbox.
- Real font packs (Japanese/Korean/Arabic scripts, licensed display fonts) —
  needs bundled TTF assets or a Google Fonts downloadable-fonts provider.
- Per-overlay opacity control (SCRL's layering tool has it; CollageX's text/
  sticker overlays don't expose one yet — needs a selection UI + slider).
- Premium/paywall billing, cloud backup, AI-credits tier, community gallery,
  shared/collab editing, Reel covers, motion collages, trend template feed.
- Firebase/Supabase backend, FastAPI AI service — no accounts or sync yet;
  everything today runs fully offline on-device.

## Build

CI builds a debug APK on every push to this branch —
`.github/workflows/build-collagex-apk.yml` → artifact `CollageX-debug-apk`.

Locally (requires Android SDK, JDK 17):
```
cd collagex-app
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

This sandbox has no Android SDK and outbound access to `dl.google.com` is
blocked, so the build could not be compiled/run locally in this session — it's
verified by careful manual review only. The first CI run on this branch is the
first real compile; watch it before assuming the app is buildable.
