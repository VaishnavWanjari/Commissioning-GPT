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
- **Export** — save to gallery (`Pictures/CollageX`) or share via the system
  share sheet, both via `MediaStore`/`FileProvider`.

## Explicitly not in v1 (see Vision doc for full scope)

These were in the original product brief but need infra this session couldn't
responsibly fake:

- Trained-model "AI" (mood detection from pixels, background replacement,
  photo cleanup / magic erase, sky replacement, skin retouch) — needs a real
  ONNX/TFLite model + training data.
- Real font packs (Japanese/Korean/Arabic scripts, licensed display fonts) —
  needs bundled TTF assets or a Google Fonts downloadable-fonts provider.
- Cloud backup, AI-credits tier, community gallery, shared/collab editing,
  Reel covers, motion collages/video export (FFmpeg), trend template feed.
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
