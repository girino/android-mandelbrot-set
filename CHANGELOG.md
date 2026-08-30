# Changelog

## Unreleased

## 1.2.0 - 2026-08-30

### Formulas and palettes

- **Phoenix**, **Julia**, **Julia Phoenix**, **Celtic Mandelbrot**, and
  **Perpendicular Mandelbrot** added to the formula picker.
- **Tricorn** label for Mandelbar (same math); formula picker scroll fixed for
  long catalogs.
- Unified **Parameters (c / p)** screen for Julia and Phoenix formulas.
- Seven new palettes: **Fire**, **Ocean**, **Grayscale**, **Sunset**, **Neon**,
  **Viridis**, **Electric** (12 palettes total).

### Session and iteration

- **Cold-start session restore** (issue #19): viewport, formula, palette, and
  smooth setting survive process death.
- Iteration settings persist independently: HUD **Reset** and formula change no
  longer wipe iteration mode/values; only **Reset to defaults** on the
  Iterations screen restores factory settings. Formula change still returns
  viewport to home.
- Defaults: Fixed / Adaptive pass-1 max **64**; adaptive absolute cap **2^18**
  (hard max **2^30**); max refinement rounds **1–31** (default **18**).

### Rendering and UX

- Fix **black band on outer escapes** at high maxIter on RGB/Neon palettes
  (issue #51).
- **Adaptive**: skip all-black progressive preview publishes and defer UI updates
  until the first border escape (issue #48).
- Smooth coloring for power-k maps uses **log(k)** escape radius (issue #40).
- **Shipbar** fixed (conjugate after abs — distinct from Burning Ship).
- Keep screen on while a render is in progress.
- Fix chained pinch during a frozen render preview; skip redundant re-render on
  resume when the frame is already ready.
- Fix false soft warning when Fixed mode max iter exceeds 4096.

### CI and tests

- CI runs **backendTestDebugUnitTest** only (no Robolectric) for reliable
  GitHub Actions runs.
- Fix cooperative worker cancel in parallel render tests (CI timeout hang).
- Shorter headless render fixtures, per-class JVM fork, render-worker shutdown
  await in gesture test tearDown.

### Issue triage

- Closed as **won't do**: #20 (accessibility), #38 (scalar remaining ops), #44
  (deep-zoom perturbation), #47 (adaptive palette), #50 (iteration-step /
  extended spatial progressive experiments).
- All GitHub issues now closed; no open backlog.

## 1.1.0 - 2026-08-24

### UI and navigation

- Bottom HUD for formula, palette, smooth, zoom −/+, and reset (issue #5);
  HUD sits above the system navigation bar.
- Zoom in/out controls and double-tap zoom at the touch point (issue #6).
- Material 3 dark theme with AppCompat; formula/palette pickers as bottom
  sheets with the current item marked (issues #10 / #13). Removed full-screen
  `ListActivity` pickers.
- Long-press shows complex coordinates; lift the finger to dismiss (issue #11).
- Removed the unreachable options menu / Exit item under NoActionBar (issue #12);
  leave via system Back or Recents.
- Edge-to-edge display: fractal draws under transparent status and navigation
  bars with light system icons; HUD and progress stay clear of chrome (issue #14).
- Adaptive launcher icon with monochrome layer for Android 13+ themed icons
  (issue #15).
- Palette picker rows show a color swatch strip from PaletteProvider (issue #16).
- Palette labels: RGB (HSB hue sweep) and BGR (blue→green→red LUT) replace
  Rainbow 1/2.
- Corner status overlay shows formula, palette, smooth coloring, iteration
  algorithm, and effective Iter on a more transparent panel; tap to hide, tap
  the chip to show again (issue #17). HUD bar background is more transparent too.
- HUD icon bar (+, −, Reset, Export) and hamburger overflow with Formula,
  Palette, Smooth (on/off), Iterations, Help, and About (issues #29 / #46).
- Help and About screens from the hamburger menu; About includes GAL license
  reference and optional external links (issue #22).

### Export, state, licensing

- Export current viewport as PNG: share sheet (FileProvider, no network) or save
  to Pictures/Fractals via MediaStore on Android 10+; older devices use the system
  save dialog (issue #18). One-tap Export on the HUD bar (issue #46).
- Rotation and activity recreate restore viewport, formula, palette, and smooth
  via saved instance state (issue #21).
- **GAL-only** licensing; drop F-Droid distribution path and BSD dual-license
  (issue #23). GitHub Releases and Zapstore remain the supported channels.

### Rendering and performance

- Determinate top progress bar while progressive render runs (issue #9),
  weighted by samples across steps 8→4→2→1.
- Parallel progressive render: row-banded workers (up to min(8, CPU cores)) fill
  each step 8→4→2→1; gesture gate and atomic handoff unchanged (issue #25).
- Formula picker rows show a mini fractal thumbnail preview (issue #30).
- Iteration settings screen: fixed max (default 40) or scale-with-zoom (base 40,
  multiplier 1.2); values persist in SharedPreferences (issue #26).
- Adaptive iteration mode (issue #28): after progressive step 1, refine only
  interior border pixels by doubling the limit each round. Iteration fields
  accept up to 1048576 (hard); values above 4096 show a soft warning. Overlay
  Iter shows the last Adaptive border limit; pass-1 is not raised on zoom.
  Adaptive recolors only retested border pixels (no full-frame palette remap).
  Doubling always starts at pass-1; early-stop on an empty border pass only
  after reaching the Adaptive max shown on the overlay (same field). Screen-edge
  perimeter is always part of the Adaptive border (every round).
- Adaptive warm-start: pass-1 stores orbit checkpoints; border retests continue
  from the previous iteration and Z via sampleContinue (no full restart).
- Adaptive border refine uses an indeterminate top progress bar (issue #31).
- Adaptive performance: separate worker pool at 2× CPU cores (cap 16) for border
  collect and retest; pass-1 stays at min(8, cores). Border seam scan runs in
  parallel; in-progress frames publish every ~4000 border samples or 250 ms
  (bitmap built on the render thread, swapped on the UI thread).
- Adaptive visited cache: while stabilizing at one limit, skip border pixels
  already sampled at that limit; clear the visited set when the iteration cap
  doubles (only newly exposed seam pixels are retested until the next double).
- Precomputed 1024-entry LUTs for Default Green/Blue/Red, HSB (RGB), and legacy
  Default palettes (same indexing as SmoothFixed / BGR) (issue #32).
- Progressive fill maps complex X by incremental column stepping (issue #34).
- Worker-local reusable EscapeSample via sampleInto / sampleContinueInto
  (issue #33; no significant FPS change on device).
- Scalar Mandelbar and Fourth Mandelbrot steps (no per-iteration Complex
  allocation) (issue #36).
- Coarse progressive fillBlock uses Arrays.fill per row for steps 8/4/2
  (issue #37).
- Fix CI hang on testDebugUnitTest: MandelbrotView render pools are daemon
  threads and tests shut them down in tearDown; unit-test task times out after
  5 minutes instead of stalling the job.

## 1.0.4 - 2026-08-22

- Fix **Smooth palette** menu toggle (issue #4): continuous iteration coloring
  now turns on/off as expected; the menu item shows a checkmark when enabled.

## 1.0.3 - 2026-08-22

- Anchor pinch zoom on the finger midpoint (issue #3): preview and committed
  bitmap keep the content under your fingers; pinch+drag walks with the focus.
- Preserve the v1.0.2 deferred-commit + atomic-handoff model (no flicker).
- Reset focus state on bitmap publish so pinch-drag is not applied twice.
- Preview may show edge gaps while transforming the stale bitmap; the new
  render always fills the screen.

## 1.0.2 - 2026-08-22

Stable release. Same fixes as 1.0.2-alpha2, validated on device.

- Eliminate flicker: gesture preview stays on screen until the new bitmap publishes (atomic handoff at first render step).
- Ignore premature pinch end when fingers get close — zoom now commits only after the last finger lifts.
- Block bitmap publication while a touch gesture is in progress; stale progressive frames never replace the screen.
- Restore the simple v1.0.0 gesture model (`ScaleGestureDetector` + canvas preview) after complex handoff attempts regressed.

## 1.0.2-alpha2 - 2026-08-22

- Eliminate flicker: gesture preview stays on screen until the new bitmap publishes (atomic handoff at first render step).
- Ignore premature pinch end when fingers get close — zoom now commits only after the last finger lifts.
- Block bitmap publication while a touch gesture is in progress; stale progressive frames never replace the screen.
- Restore the simple v1.0.0 gesture model (`ScaleGestureDetector` + canvas preview) after complex handoff attempts regressed.

## 1.0.1 - 2026-08-22

- Fix pinch-zoom viewport jumps and keep the fractal point under your fingers (issue #2).
- Smooth pinch and pan: manual two-finger tracking, canvas preview during gestures, progressive re-render after release.
- Fix pan speed when a zoom preview is active and avoid blank areas during gesture handoff.
- Append a compile timestamp to debug build version names for on-device identification.

## 1.0.1-alpha - 2026-08-22

- Fix abrupt viewport jumps during pinch zoom (issue #2).
- Anchor pinch commits on the gesture focus and keep preview transforms until re-render.
- Modernize unit tests to JUnit 5 and expand coverage, including viewport regression tests.

## 1.0.0 - 2026-08-14

- Add BSD 2-Clause as an F-Droid-compatible alternative to the GAL.
- Add localized F-Droid metadata, icon, and current FOSS screenshots.
- Document the F-Droid build and submission process.
- Promote the modernized FOSS edition from alpha to its first stable release.

## 1.0.0-alpha - 2026-08-13

- Migrate the project from Eclipse/ADT to a modern Gradle build.
- Target Android 16 while retaining support for Android 5.0 and newer.
- Remove AdMob, advertising metadata, and Internet and location permissions.
- Make rendering lifecycle-safe and non-blocking.
- Harden multitouch pointer handling.
- Use a distinct FOSS application name and package ID for parallel installation.
