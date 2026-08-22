# Postmortem — pinch anchor (issue #3) attempts

Status: **resolved and shipped in v1.0.3** (2026-08-22).
Closes [#3 — Pinch zoom is anchored to screen center](https://github.com/girino/android-mandelbrot-set/issues/3).

This document records what was tried, why early attempts failed, and the
constraints of the fix that shipped. Read it together with
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
3. **The NEW recalculated bitmap fills the whole window** — after deferred
   commit + atomic handoff, the published render covers the viewport. Gaps
   during the live preview (stale bitmap transformed on the canvas leaving
   background at the edges) are **acceptable** (product clarification,
   2026-08-22, recorded on issue #3). Do not reject a focus-following preview
   only because it leaves gaps.
4. **No rendering and no publishing mid-gesture** — flicker model of v1.0.2
   (deferred commit + atomic handoff) stays intact.
5. **Premature `onScaleEnd` and detector restarts must not jump the preview**
   — the detector can end early when fingers get close (~1 cm) and a new
   POINTER_DOWN restarts it; accumulation continues into the same preview.

Earlier drafts treated "preview must always fill the window" as hard. That was
wrong: it blocked the standard Matrix/`postScale(..., focus)` pattern and
pushed attempt 3 into an unnecessary rejection. Gaps on the stale-bitmap
preview are fine; only the new full-window render is mandatory.

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
  no longer filled the window and the overall feel was wrong.
- **Why it failed then / what changed**: at the time we treated "preview must
  fill the window" as hard. Product clarification (issue #3, 2026-08-22) later
  said gaps during preview are acceptable; only the new published bitmap must
  fill the screen. Attempt 3's math (preview==commit) remains useful; the
  covering-constraint rejection no longer applies. Revisit the Affine/
  Matrix focus-following approach with that constraint relaxed.

## Rollback (historical)

`git checkout v1.0.2 -- app/src` restored `MandelbrotView.java`,
`ViewportTransforms.java`, and both test files to the released state after
failed attempts. Tag `pre-issue3-focus-anchor` marked the tree immediately
before the successful re-implementation.

## What shipped in v1.0.3

Affine focus-following preview (Matrix-style):

```text
q = position + focus + s * (p - startFocus)
```

`ViewportTransforms.commitAffinePreview` derives the pending target from the
same map so preview and commit agree everywhere. Detector restarts shift
`startFocus` to keep the map continuous. Atomic handoff resets
`focus`/`startFocus` to screen center with the rest of the preview — otherwise
with `s=1` the leftover `(focus - startFocus)` re-applies pinch-drag on top of
the new bitmap (~2x movement). Gaps during the stale-bitmap preview are
accepted; the published render fills the window.

## Lessons

1. **Gaps during stale-bitmap preview are OK** (issue #3 clarification).
   Only the new published render must fill the window. Do not reject a
   focus-following transform for edge gaps alone.
2. **Preview and commit must share one transform.** Property tests that
   assert preview==target at many screen points catch algebra bugs that
   pixel-looking tests miss.
3. **Robolectric detector quirks are real**: late begin, begin-at-moved-focus,
   first-MOVE-bundles-motion. Use warm-up MOVEs in simulators; do not assume
   `onScaleBegin` sees the touch-down geometry.
4. **Do not re-anchor a live preview** on detector restart; shift the map's
   parameters to stay continuous.
5. **Clear ALL preview state on atomic handoff** — including focus anchors,
   not only `accumulatedScale`/`positionX/Y`.
6. **User-visible acceptance beats green tests**: encode product constraints
   (gaps OK / fill after publish) explicitly so agents do not invent blockers.
