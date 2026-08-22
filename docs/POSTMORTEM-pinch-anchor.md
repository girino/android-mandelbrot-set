# Postmortem — pinch anchor (issue #3) attempts

Status: **rolled back to v1.0.2 behavior on 2026-08-22**. Issue #3 remains open.
This document records what was tried, why each attempt failed, and what any
future fix must respect. Read it together with
[POSTMORTEM-viewport-gestures.md](POSTMORTEM-viewport-gestures.md) and
[POSTMORTEM-viewport-flicker.md](POSTMORTEM-viewport-flicker.md).

## Problem

In v1.0.2, pinch zoom scales around the **screen center**, not around the
pinch focal point (the midpoint between the fingers). Users expect the content
under their fingers to stay under their fingers while zooming
([issue #3](https://github.com/girino/android-mandelbrot-set/issues/3)).

## Hard constraints (learned the hard way)

Any fix must satisfy ALL of these simultaneously:

1. **Preview follows the live pinch midpoint** — the content walks with the
   fingers while they move (user requirement, did not exist before).
2. **Commit matches the preview exactly** — the rendered bitmap after release
   shows, at every screen pixel, the same complex coordinate the preview
   showed. No jump at handoff.
3. **The bitmap fills the whole window at all times** — the preview is a
   canvas transform of the *published* bitmap; the user rejects previews where
   the bitmap no longer covers the viewport (background/gaps visible).
4. **No rendering and no publishing mid-gesture** — flicker model of v1.0.2
   (deferred commit + atomic handoff) stays intact.
5. **Premature `onScaleEnd` and detector restarts must not jump the preview**
   — the detector can end early when fingers get close (~1 cm) and a new
   POINTER_DOWN restarts it; accumulation continues into the same preview.

Constraint 3 is the killer: a translated/scaled stale bitmap only covers the
screen within bounds (`|translation| <= (s-1)/2 * size` per axis for zoom-in;
zoom-out never covers unless anchored at center). A free focus-following
transform violates it as soon as the midpoint drifts more than the zoom slack
— which is exactly the gesture the feature is for.

## Timeline of attempts (all on top of v1.0.2)

### Attempt 1 — anchor the preview at the moving focus (commit 366c066)

`onDraw` used `dx = (1-s) * focusX` with `focusX/Y` updated on every
`onScale`; commit used `ViewportTransforms.commitFrozenGesture` with the
end-of-gesture focus as invariant point.

- **Result**: zoom-out looked right; zoom-in moved the image *against* the
  fingers. Preview and commit disagreed (different invariant points).
- **Why it fails**: multiplying by a moving anchor makes the translation term
  itself move; there is no invariant point at all. The commit assumed one.

### Attempt 2 — freeze the anchor at `onScaleBegin`

Introduced `anchorX/Y` captured once at gesture start; `focusX/Y` kept updating
only for the commit.

- **Result**: worse. Preview only zoomed about a fixed point (no walking),
  then the image jumped to the committed position on release. User: "a emenda
  foi pior que o soneto" and clarified the desired behavior: preview walks
  with the pinch center; bitmap matches the preview.
- **Why it fails**: preview invariant (start anchor) != commit invariant
  (end focus). Two different transforms guaranteed a mismatch.

### Attempt 3 — affine focus-following map (commit 4a42593)

New model: `q = position + focus + s * (p - startFocus)` — scale about the
focus at gesture start, translate so that starting point stays under the live
midpoint. `ViewportTransforms.commitAffinePreview` derived the pending target
from the same matrix, making preview and commit mathematically identical.
Detector restarts shifted `startFocus` to keep the map continuous.

Along the way three separate bugs were found and fixed by the new tests:

- First draft composed "scale about current focus + drag by focus delta" —
  no invariant anywhere (tests caught preview/commit divergence).
- Translation fed to the commit omitted the `s *` on `startFocus`
  (`d = pos + focus - s*startFocus`, not `pos + focus - startFocus`) — broke
  even the centered case until fixed.
- Robolectric's `ScaleGestureDetector` starts late and can fire
  `onScaleBegin` again at a moved focus; the first MOVE bundles begin + span +
  motion. Tests need a warm-up no-motion MOVE; the view needs continuity
  handling on restart (kept — harmless on device).

Final state: 90/90 tests green, including field-equality probes
(preview == target at many screen points) and moving-focus tracking.

- **Result on device**: rejected by the user — the transformed stale bitmap
  no longer filled the window (constraint 3). Behavior felt broken.
- **Why it fails**: fundamental, not a bug. Constraint 1+2 are achievable
  with a stale-bitmap transform; constraint 3 caps how far that transform may
  translate/shrink the bitmap. A free focus-following gesture exceeds the cap.

## Rollback

`git checkout v1.0.2 -- app/src` restored `MandelbrotView.java`,
`ViewportTransforms.java`, and both test files to the released state. Tests
and build verified, APK installed on device. Commits 366c066 and 4a42593
remain in history but their code changes are reverted in a follow-up commit.

## Lessons / rules for the next attempt

1. **Decide the covering constraint first.** If the preview must always fill
   the window, the transform space is small: essentially scale about a point,
   with translation clamped to the slack the scale factor provides. Either
   clamp focus-following to that slack (content lags fingers at extremes), or
   abandon the stale-bitmap preview.
2. **The clean fix probably needs a different preview source**: render a
   cheap coarse bitmap *of the target viewport* continuously (throttled)
   during the gesture instead of transforming the old one — that satisfies
   all five constraints by construction, at CPU cost. This mirrors what the
   flicker postmortem called "publish intermediate steps", which failed for
   flicker reasons; any revival must keep the generation gate and publish
   atomically.
3. **Property tests before pixels.** The preview==target field-equality probe
   caught two algebra bugs immediately. Write it first for any new model.
4. **Robolectric detector quirks are real**: late begin, begin-at-moved-focus,
   first-MOVE-bundles-motion. Use warm-up MOVEs in simulators; do not assume
   `onScaleBegin` sees the touch-down geometry.
5. **Do not re-anchor a live preview** on detector restart; shift the map's
   parameters to stay continuous (the one piece of attempt 3 worth keeping).
6. **User-visible acceptance beats green tests**: all 90 tests passed and the
   behavior was still wrong. Constraints came from product feedback, not
   code. Encode them as tests only after the user confirms them.
