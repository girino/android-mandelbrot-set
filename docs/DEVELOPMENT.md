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
`1.0.2-20260822120000`, so you can tell builds apart on a device.

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

Known limitation: Robolectric's `ScaleGestureDetector` does not reproduce real
pinch faithfully, so full-gesture tests are conservative. Always validate
gesture changes on a physical device.

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
docs/                          usage, deploy, postmortems
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
  stable; any other tag publishes as pre-release.
