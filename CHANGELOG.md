# Changelog

## Unreleased

- Bottom HUD for formula, palette, smooth, zoom −/+, and reset (issue #5);
  HUD sits above the system navigation bar.
- Zoom in/out controls and double-tap zoom at the touch point (issue #6).
- Determinate top progress bar while progressive render runs (issue #9),
  weighted by samples across steps 8→4→2→1.
- Material 3 dark theme with AppCompat; formula/palette pickers as bottom
  sheets with the current item marked (issues #10 / #13). Removed full-screen
  `ListActivity` pickers.
- Long-press shows complex coordinates; lift the finger to dismiss (issue #11).
- Removed the unreachable options menu / Exit item under NoActionBar (issue #12);
  leave via system Back or Recents.
- Edge-to-edge display: fractal draws under transparent status and navigation
  bars with light system icons; HUD and progress stay clear of chrome (issue #14).
- **GAL-only** licensing; drop F-Droid distribution path and BSD dual-license
  (issue #23). GitHub Releases and Zapstore remain the supported channels.
- Adaptive launcher icon with monochrome layer for Android 13+ themed icons
  (issue #15).
- Palette picker rows show a color swatch strip from PaletteProvider (issue #16).
- Palette labels: RGB (HSB hue sweep) and BGR (blue→green→red LUT) replace Rainbow 1/2.
- Corner status overlay shows formula, palette, smooth coloring, iteration
  algorithm, and effective Iter on a more transparent panel; tap to hide, tap
  the chip to show again (issue #17). HUD bar background is more transparent too.
- Export current viewport as PNG: share sheet (FileProvider, no network) or save
  to Pictures/Fractals via MediaStore on Android 10+; older devices use the system
  save dialog (issue #18).
- HUD icon bar (+, −, Reset, Smooth) and hamburger overflow menu with icon + label
  rows for all actions including Formula, Palette, and Export (issue #29).
- Rotation and activity recreate restore viewport, formula, palette, and smooth
  via saved instance state (issue #21).
- Help and About screens from the hamburger menu; About includes GAL license
  reference and optional external links (issue #22).
- Iteration settings screen: fixed max (default 40) or scale-with-zoom (base 40,
  multiplier 1.2); values persist in SharedPreferences (issue #26).
- Parallel progressive render: row-banded workers (up to min(8, CPU cores)) fill
  each step 8→4→2→1; gesture gate and atomic handoff unchanged (issue #25).
- Formula picker rows show a mini fractal thumbnail preview (issue #30).
- Adaptive iteration mode: after progressive step 1, refine only interior
  border pixels by doubling the limit each round (issue #28). Iteration
  fields accept up to 1048576 (hard); values above 4096 show a soft warning.
  Overlay Iter shows the last Adaptive border limit; pass-1 is not raised on zoom.
  Adaptive recolors only retested border pixels (no full-frame palette remap).
  Doubling always starts at pass-1; early-stop on an empty border pass only
  after reaching the Adaptive max shown on the overlay (same field). maxRounds
  Screen-edge perimeter is always part of the Adaptive border (every round).
  Adaptive border refine uses an indeterminate top progress bar (issue #31).
- Adaptive warm-start: pass-1 stores orbit checkpoints; border retests continue
  from the previous iteration and Z via sampleContinue (no full restart).
- **Experiment (branch experiment/adaptive-parallelism):** Adaptive border
  refine uses a separate worker pool at 4× CPU cores (cap 32); pass-1 unchanged.
  Border seam scan runs in parallel; in-progress frames publish every ~1000
  border samples or 100 ms (bitmap built on render thread, swapped on UI).

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
