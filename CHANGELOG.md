# Changelog

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
