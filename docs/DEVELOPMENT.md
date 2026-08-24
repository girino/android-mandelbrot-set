# Development guide

How to build, test, and work on Fractals by Girino FOSS.

## Toolchain (local to the repo)

The repo carries its own JDK and Android SDK so builds are reproducible and do
not touch the global system:

| Tool | Location | Notes |
|------|----------|-------|
| JDK 17+ | `.jdk/` | Temurin; set `JAVA_HOME` here |
| Android SDK | `.android-sdk/` | Platform 36, Build-Tools 36.0.0, platform-tools (adb) |
| AVDs (optional) | `.android-avd/` | Set `ANDROID_AVD_HOME` |

First-time setup:

```powershell
.\scripts\setup-android-sdk.ps1        # installs SDK packages into .android-sdk\
.\scripts\setup-headless-tests.ps1     # verifies env for Robolectric tests
```

Never commit `.jdk/`, `.android-sdk/`, or `local.properties`.

## Building

From the repo root on Windows (PowerShell):

```powershell
$env:JAVA_HOME = Join-Path $PWD ".jdk"
$env:ANDROID_HOME = Join-Path $PWD ".android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME

.\gradlew.bat assembleDebug          # debug APK -> app\build\outputs\apk\debug\
.\gradlew.bat assembleRelease        # release APK (signed only if keystore env vars set)
```

Debug builds append a compile timestamp to `versionName`, e.g.
`1.1.0-20260824120000`, so you can tell builds apart on a device.

## Testing

All tests run headless on the JVM — no emulator needed.

```powershell
.\gradlew.bat testDebugUnitTest
# or
.\scripts\setup-headless-tests.ps1 -RunTests
```

| Suite | What it covers |
|-------|----------------|
| `ViewportTransformsTest` | Pure viewport math (pan/pinch commits, preview bridge) — reliable |
| `MandelbrotViewGestureTest` | Gesture flows via Robolectric + `PinchDragMotionSimulator` |
| `CatalogTest` | Formula/palette catalog labels and index lookup |
| `ParallelStepRendererTest` | Parallel vs serial pixel match, cancel mid-step, worker counts, fillBlock edges (issues #25 / #37) |
| `AdaptiveRefinerTest` | Border collect, seed floor, warm-start refine vs brute force, visited-cache drop (issue #28) |
| `FractalOperatorContinueTest` | sampleContinue / sampleInto parity for all UI operators (issue #33) |
| `MandelbarFourthScalarStepTest` | Scalar Mandelbar/Fourth match legacy Complex step (issue #36) |
| `PaletteLutTest` | Default/HSB LUT matches formula at sample points (issue #32) |
| `IterationSettingsStoreTest` | SharedPreferences persistence for iteration modes |

Known limitation: Robolectric's `ScaleGestureDetector` does not reproduce real
pinch faithfully, so full-gesture tests are conservative. Always validate
gesture changes on a physical device.

### Parallel render (issue #25)

`MandelbrotView` keeps a single coordinator thread for progressive publish
(steps 8→4→2→1) and a fixed worker pool sized
`min(8, availableProcessors())`. Each step is partitioned by sample rows;
workers write non-overlapping bands into an `int[]` buffer, then the
coordinator calls `Bitmap.setPixels` and posts the existing atomic handoff.
Each worker gets a fresh `FormulaCatalog.createLike(...)` operator because
`FractalOperator` holds mutable iteration state.

### Adaptive refine (issues #28 / #31)

After progressive step 1 in Adaptive mode:

- Pass-1 fills `interior[]` and `OrbitState` checkpoints (warm-start).
- `AdaptiveRefiner.refine()` runs on the render coordinator thread; border
  **collect** and **retest** use a separate `adaptiveWorkerPool` sized
  `min(16, 2× cores)`.
- Throttled `PreviewListener` publishes in-progress frames (~4000 border
  samples or 250 ms) without blocking workers.
- Indeterminate progress bar while refine runs (issue #31).
- Per-limit visited bitmap: border pixels already probed at the current cap
  are skipped until the limit doubles (see `dropVisitedBorder` in
  `AdaptiveRefiner`).

Algorithm and tuning notes: [ADAPTIVE-ITERATION.md](ADAPTIVE-ITERATION.md).
Do not publish bitmaps or start renders while a gesture is active — same gate
as progressive fill.

## Lint

CI runs `lintRelease` and **a red lint blocks the release**. One known trap:
the lint JavaDoc parser crashes (`NoSuchMethodError`) on inline JDoc tags.
**Never use `{@code ...}`, `{@link ...}` or similar tags in comments under
`app/src/main/**`** — write plain text. Reference fixes: commits `d2e262a`
and `bae4e88`. Run locally before tagging:

```powershell
.\gradlew.bat lintRelease testDebugUnitTest
```

## Installing on a device

```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

If adb reports no devices: restart the server
(`adb kill-server; adb start-server`) and re-check.

## Code layout

UI stack: AppCompat + Material Components (dark theme, bottom sheets,
`EdgeToEdge`). Dependencies are declared in `app/build.gradle.kts`.

```
app/src/main/java/org/girino/frac/
  android/foss/
    MandelbrotActivity.java    main screen, HUD, pickers, insets, edge-to-edge
    MandelbrotView.java        fractal rendering surface + gestures (the hot file)
    AdaptiveRefiner.java       border-doubling adaptive iteration (issue #28)
    OrbitState.java            per-pixel orbit checkpoints for warm-start
    ParallelStepRenderer.java  parallel progressive step fill (issue #25)
    IterationSettings.java     fixed / scale-with-zoom / adaptive policy
    FormulaCatalog.java        formula labels + operators for the picker
    PaletteCatalog.java        palette labels + providers for the picker
  operators/                   FractalOperator implementations (z <- f(z, c))
  palettes/                    PaletteProvider implementations + ARGB helpers
  viewport/
    ViewportTransforms.java    pure viewport math shared with tests
app/src/test/java/org/girino/frac/
  viewport/ViewportTransformsTest.java
  android/foss/MandelbrotViewGestureTest.java
  android/foss/PinchDragMotionSimulator.java
  android/foss/CatalogTest.java
  android/foss/AdaptiveRefinerTest.java
  android/foss/ParallelStepRendererTest.java
  operators/FractalOperatorContinueTest.java
docs/                          usage, deploy, postmortems, ADAPTIVE-ITERATION.md
```

### The gesture/render model (read before touching MandelbrotView)

`MandelbrotView` implements **deferred commit + atomic handoff**:

1. During a gesture, the view draws a canvas-transformed *preview*
   (`accumulatedScale`, `positionX/Y`, live `focus` / `startFocus`) of the
   already-published bitmap. Pinch scales about the finger midpoint so
   content walks with the pinch (issue #3). Nothing is rendered or published
   while pointers are down.
2. The logical viewport (`centerX/Y/scale`) keeps describing the published
   bitmap until the last finger lifts.
3. On last-pointer-up, `commitGestureAndRender()` folds preview + gesture into
   a pending target (`commitAffinePreview`) and starts the render against it.
4. When the render finishes, one atomic `post()` publishes the new bitmap AND
   the new viewport AND clears the preview (including focus anchors) — the
   old bitmap is never exposed, and pinch-drag is not applied twice.

This model exists because six earlier approaches flickered; see
[POSTMORTEM-viewport-flicker.md](POSTMORTEM-viewport-flicker.md) and
[POSTMORTEM-pinch-anchor.md](POSTMORTEM-pinch-anchor.md) before changing
anything. Hard requirements live in
[`.cursor/rules/viewport-smooth-transition.mdc`](../.cursor/rules/viewport-smooth-transition.mdc)
and regression tests in `MandelbrotViewGestureTest`.

## Versioning and releases

- `versionCode` / `versionName` in `app/build.gradle.kts`; every user-visible
  change gets an entry in [CHANGELOG.md](../CHANGELOG.md).
- A pushed tag triggers the signed release build (see
  [DEPLOY.md](DEPLOY.md)). Tags matching `vMAJOR.MINOR.PATCH` publish as
  stable; any other tag publishes as pre-release. Zapstore publish is manual
  from WSL after the GitHub Release — details in DEPLOY.md (not in the README).
- New agent continuing after v1.1.0: start at [HANDOFF.md](HANDOFF.md).
  Release retrospective: [POSTMORTEM-1.1.0.md](POSTMORTEM-1.1.0.md).
