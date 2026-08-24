# Postmortem — Adaptive iteration (issue #28) in v1.1.0

**Status:** shipped in v1.1.0 (2026-08-24). Algorithm reference:
[ADAPTIVE-ITERATION.md](ADAPTIVE-ITERATION.md). Release context:
[POSTMORTEM-1.1.0.md](POSTMORTEM-1.1.0.md).

Closes [#28](https://github.com/girino/android-mandelbrot-set/issues/28);
follow-ups [#31](https://github.com/girino/android-mandelbrot-set/issues/31)
(indeterminate progress), warm-start, visited cache, worker pools.

This document records **product/algorithm wrong turns** so the next agent does
not repeat them. The current model is border doubling + warm-start + perimeter
seed + stop-floor — not "raise pass-1 max from last zoom" and not "remap the
whole palette every round."

---

## Summary

| Item | Value |
|------|-------|
| Goal | Spend iterations where the set boundary is, not on deep interior |
| Chosen algorithm | After progressive step 1, double limit on **interior border** only |
| Hard constraint | Same as viewport: **no publish / no start** with `activePointers > 0` |
| Core types | `AdaptiveRefiner`, `OrbitState`, `IterationSettings` (Adaptive mode) |

## What works (do not casually rewrite)

1. Pass-1 at configured Fixed max → `interior[]` + `OrbitState` checkpoints.
2. Border = interior pixels with an escaped 4-neighbor **union image perimeter**
   (every round). All-interior frames still refine from the screen edge.
3. Stabilize at a limit until a pass finds no new escapes; then double
   (`next = min(2 × current, absoluteCap)`).
4. Early-stop on empty pass only when `current >= seedMinStopIter`
   (overlay `adaptiveMaxIter` from the previous finished refine / zoom).
5. Recolor **only** retested border pixels.
6. Warm-start via `sampleContinue` / `sampleContinueInto`.
7. `visitedAtLimit`: skip pixels already probed at the **same** limit; clear
   when the limit doubles.
8. Pools: pass-1 `workerPool` min(8, cores); Adaptive
   `adaptiveWorkerPool` min(16, 2× cores). Preview throttle ~4000 px / 250 ms.

## Failed or reverted approaches

### 1. Carry last Adaptive max into the next zoom's pass-1

Tried raising progressive maxIter from the previous frame's border max so deep
zooms would "start already refined."

- **Why it hurt:** pass-1 became hugely expensive; overlay semantics tangled;
  progressive black/coarse frames got worse, not better.
- **Outcome:** Dropped. Pass-1 stays at configured Fixed max. Overlay **Iter**
  is display + Adaptive **stop-floor**, not the next pass-1 budget
  (`34118fb` and related).

### 2. Full-frame palette remap on each border round / zoom

Tried remapping all escaped pixels to a shared iteration max so colors stayed
"comparable" across rounds (`648c285`).

- **Why it hurt:** flashing / rewriting the whole image; expensive; fought the
  "only border pixels change" invariant.
- **Outcome:** Reverted (`0ed1b2a`). Escaped pixels keep the color from the
  pass that first found them.

### 3. Incremental frontier `collectBorder` (#35)

Track previous border + neighbors instead of full-frame scan.

- **Why it hurt:** On device, slower than full scan (extra bitmaps
  `newlyEscaped` / `collectCandidates`).
- **Outcome:** Abandoned; issue closed; branch discarded. Parallel **row-banded
  full scan** on `adaptiveWorkerPool` is the current collect strategy.

### 4. Over-tuned preview / pool sizes

1000 px / 100 ms previews and 4×-core (cap 32) Adaptive pool.

- **Why it hurt:** UI thrash / diminishing returns.
- **Outcome:** Settled on ~4000 / 250 ms and 2× cores cap 16 (`7c32fe3`).

## Naming / semantics traps

| Name | Meaning |
|------|---------|
| `seedMinStopIter` | Minimum limit before an **empty** border pass may stop refine |
| Overlay `adaptiveMaxIter` | Last published border limit; becomes next zoom's stop-floor |
| `IterationSettings.fixedMax` in Adaptive mode | Pass-1 (and only pass-1) iteration cap |

When reading older commits, "seed" / "carry" / "zoom max" often mean different
things — prefer current names in `AdaptiveRefiner` / `MandelbrotView`.

## Open Adaptive UX debt

Deep zoom: progressive steps at Fixed max are often **all interior → black**.
They still publish today, wiping a useful stale preview until border refine
paints. Experimental fix: [#48](https://github.com/girino/android-mandelbrot-set/issues/48).
Related color stretch idea: [#47](https://github.com/girino/android-mandelbrot-set/issues/47).

Any fix must:

- Still compute pass-1 interiors / `OrbitState` (Adaptive needs them).
- Gate **UI bitmap publish**, not the math.
- Preserve deferred-commit + atomic handoff and generation checks.

## Tests

- `AdaptiveRefinerTest` — border collect, stop-floor, warm-start vs brute,
  visited-cache drop.
- `FractalOperatorContinueTest` — continue vs restart parity.
- After gesture-adjacent Adaptive UI changes: `MandelbrotViewGestureTest`.

## Do not

- Revive #35 incremental collect without device proof.
- Remap the full palette every Adaptive round.
- Start or publish renders with pointers down.
- Raise pass-1 max from overlay Iter "to go faster."
- Assume Robolectric FPS equals device FPS for Adaptive opts.
