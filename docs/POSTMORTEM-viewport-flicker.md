# Postmortem — viewport flicker fix (deferred commit + atomic handoff)

**Status:** resolved and shipped in v1.0.2 (2026-08-22).
Closes [#2 — Pinch zoom causes abrupt viewport movement](https://github.com/girino/android-mandelbrot-set/issues/2).

Companion document: [POSTMORTEM-viewport-gestures.md](POSTMORTEM-viewport-gestures.md)
covers the *failed* approaches. This one covers the fix that worked, why it
works, and what it cost.

---

## Summary

| Item | Value |
|------|-------|
| Symptom | Bitmap flash/jump during pan/pinch; premature zoom commit when fingers came close |
| Root cause | Progressive render published stale-viewport bitmaps over the live preview; preview was reset on `onScaleEnd`, which fires early (~1 cm finger proximity) |
| Fix | **Deferred commit + atomic handoff** in `MandelbrotView` (~40 lines net) |
| Shipped in | `v1.0.2-alpha2` (`eb14679`+`bae4e88`) → stable `v1.0.2` (`418a8ea`) |
| Validation | Robolectric gesture tests + on-device manual tests by the author across all reported scenarios |

## Timeline

| When | Event |
|------|-------|
| 2026-08-14 | #2 filed from F-Droid review testing on Android 16 |
| 2026-08-22 (early) | Six failed attempts (see [gestures postmortem](POSTMORTEM-viewport-gestures.md)); code rolled back to the v1.0.0 model |
| 2026-08-22 | User reframed the requirement: *"do not publish a NEW bitmap while the preview is active; do not even start computing one"* + report that pinch ended while fingers were still down |
| 2026-08-22 | New model implemented: no render during gesture, commit deferred to last-pointer-up, atomic bitmap+viewport swap at publish. Validated on device |
| 2026-08-22 | Released as `v1.0.2-alpha2`; CI lint crash fixed (`bae4e88`); promoted to stable `v1.0.2` same day |

## Root cause analysis

Two independent defects compounded:

1. **Stale publish during gesture.** The render thread loops progressively
   (step 8→4→2→1) and each iteration ran
   `post(() -> { bitmap = rendered; invalidate(); })`. A render started for an
   older viewport kept replacing the base texture *while* `onDraw` drew a
   canvas-transformed preview on top of it. The screen showed: preview of old
   bitmap → sudden untransformed old/coarse bitmap → preview again = flicker.
   The earlier "pause rendering" attempt failed because it blocked only some
   paths; the "gate publishes" attempt broke pinch preview because it also
   gated state updates the preview depended on.

2. **Premature gesture end.** `ScaleGestureDetector.onScaleEnd` fires when
   finger span shrinks below ~1 cm — long before fingers lift. The old flow
   committed scale and cleared the preview there, exposing the stale bitmap
   mid-gesture (worst on zoom-out, where fingers converge).

The deep lesson from six failures: every attempt tried to make *some* new
bitmap appear faster or smarter. All of them exposed the old texture at some
instant. The correct invariant is the opposite:

> While a gesture is active, nothing about the displayed content changes.
> The switch to new content happens exactly once, atomically, and only after
> the new bitmap exists.

## The fix

In `MandelbrotView.java` (v1.0.0 gesture skeleton preserved):

1. **Gate rendering during gestures** — `start()` refuses to run with pointers
   down; a running render aborts if a gesture begins (`renderGeneration`
   counter discards results).
2. **Track real gesture lifetime** — `activePointers` counts
   `ACTION_DOWN`/`POINTER_DOWN` vs `POINTER_UP`/`UP`/`CANCEL`.
   `onScaleEnd` is now a **no-op**: commit happens only when
   `activePointers == 0`.
3. **Deferred commit** — on last-pointer-up,
   `commitGestureAndRender()` folds the frozen preview (`accumulatedScale`,
   `positionX/Y`) plus the gesture into `targetCenterX/Y/scale`
   (`hasPendingTarget`). Until publish, `centerX/Y/scale` keep describing the
   bitmap on screen, so any new gesture starts from consistent coordinates.
4. **Atomic handoff** — when the target render's first step completes, one
   `post()` does all of: `bitmap = rendered`, `centerX/Y/scale = target*`,
   clear pending target, reset `accumulatedScale = 1`,
   `positionX/Y = 0`. Preview never disappears before its replacement is
   already on screen.

Net effect: between gesture start and first target publish, the screen shows a
constant bitmap under a continuously-updated canvas transform — nothing can
flash.

## What made this attempt succeed where six failed

- **Invariant-first design.** Instead of tuning *when* bitmaps swap, we made
  swaps impossible during gestures. One invariant, enforced in two places
  (`start()` and the render loop), instead of many coordinated flags.
- **Reusing the proven v1.0.0 gesture core.** After the rollback, `ScaleGestureDetector`
  and the simple preview stayed untouched; fixes were additive gates around
  them, not rewrites.
- **Committing on physical events, not detector callbacks.** Pointer count is
  ground truth; `onScaleEnd`'s heuristic is ignored.
- **Testing state transitions, not pixels.** Robolectric cannot reproduce
  flicker visually, so tests assert the invariants directly:
  no publish with `activePointers > 0`; pending target created exactly once;
  preview values survive until publish; stale generations dropped
  (`MandelbrotViewGestureTest`, hooks like `testingActivePointers()`,
  `testingHasPendingTarget()`).
- **User validation at each step.** On-device install after each change; the
  user confirmed scenarios that previously flickered (pan, pinch, zoom-out
  with converging fingers, chained fast gestures) before anything was tagged.

## Costs / trade-offs

- First visible refinement after release arrives at coarse resolution (the
  target's step 8), refining in place afterwards — imperceptible vs. the
  previous flashing.
- During long renders after a gesture, a new gesture restarts targeting from
  the *published* viewport (correct, but means abandoning partial work).
- ~10 package-private `testing*` hooks added for JVM tests; harmless but
  slightly widen the class surface.

## Follow-ups

- [x] Regression suite committed (`MandelbrotViewGestureTest`)
- [x] Model documented as mandatory rule (`.cursor/rules/viewport-smooth-transition.mdc`)
- [x] Focus-anchored pinch zoom (issue #3) — shipped in v1.0.3; see
  [POSTMORTEM-pinch-anchor.md](POSTMORTEM-pinch-anchor.md)
