# Postmortem — release v1.1.0

**Status:** shipped 2026-08-24.
**Tag / release:** [`v1.1.0`](https://github.com/girino/android-mandelbrot-set/releases/tag/v1.1.0)
(`versionCode` 9, `versionName` 1.1.0). Also published to Zapstore.

This document is the **release retrospective**: what landed, what was tried and
reverted, and lessons for the next cycle. Algorithm detail for Adaptive lives in
[ADAPTIVE-ITERATION.md](ADAPTIVE-ITERATION.md) and
[POSTMORTEM-adaptive-iteration.md](POSTMORTEM-adaptive-iteration.md). Agent
onboarding: [HANDOFF.md](HANDOFF.md).

---

## Summary

| Item | Value |
|------|-------|
| Scope | Large UI/Material/HUD cycle + Adaptive iteration + parallel render + micro-opts |
| Prior line | v1.0.4 (Smooth toggle); viewport model frozen since v1.0.2 / pinch since v1.0.3 |
| Validation | `lintRelease` + `testDebugUnitTest` (repo-local JDK/SDK); on-device for Adaptive / opts |
| Distribution | GitHub Release APK + sha256; Zapstore via WSL `zsp` |

## What shipped (themes)

### Product / UI

- Bottom HUD, Material 3 dark theme, bottom-sheet pickers, edge-to-edge, status
  overlay, Help/About, PNG export, rotation restore (`ViewportSession`).
- HUD bar ends at **+ − Reset Export** + hamburger; Smooth lives in overflow
  with on/off (#5, #6, #10–#18, #21–#23, #29, #46).
- GAL-only licensing; F-Droid path dropped (#23).

### Rendering

- Determinate progress for progressive 8→4→2→1 (#9); indeterminate during
  Adaptive border refine (#31).
- Parallel pass-1 workers (`workerPool`, min(8, cores)) (#25).
- **Adaptive** border-doubling after step 1 (#28): warm-start `OrbitState`,
  perimeter always in border, stop-floor from overlay `adaptiveMaxIter`,
  visited-at-limit cache, separate `adaptiveWorkerPool` (min(16, 2× cores)),
  throttled `PreviewListener` (~4000 px / 250 ms). See Adaptive postmortem.

### Micro-optimizations (kept)

| Issue | Change | Device note |
|-------|--------|-------------|
| #32 | LUT for Default/HSB family palettes | Clear win for those palettes |
| #34 | Incremental `cRe` in `fillRowRange` | Cheap, kept |
| #33 | Reusable `EscapeSample` / `sampleInto` | **No measurable FPS**; kept for GC; revert steps in class Javadoc |
| #36 | Scalar Mandelbar / Fourth | Kept |
| #37 | `Arrays.fill` in coarse `fillBlock` | Kept |

### Tooling / ops

- CI hang on `testDebugUnitTest`: non-daemon `MandelbrotView` pools kept the
  JVM alive. Fix: daemon thread factories + shut down pools in test tearDown /
  `testingReleaseBitmap()`; Gradle unit-test timeout 5 min (`0c9e14c` /
  changelog fold `fea7994`).
- Zapstore: credentials only in gitignored `.credentials.env` with
  **quoted** `SIGN_WITH='bunker://...?&...'` (unquoted `&` breaks bash
  `source`). Rules: `zapstore-wsl.mdc`, `docs/DEPLOY.md`.
- README trimmed to end-user content (`readme-user-facing.mdc`); release how-to
  stays in `DEPLOY.md`.

## Explicitly abandoned

| Item | Why |
|------|-----|
| [#35](https://github.com/girino/android-mandelbrot-set/issues/35) incremental `collectBorder` | On device **slower** than full-frame scan (extra bitmaps / candidate bookkeeping). Branch discarded (`d872297`). Do not revive without new evidence. |
| Adaptive **full-frame palette remap** each border round | Tried (`648c285`); reverted (`0ed1b2a`). Only retested border pixels change color. |
| Aggressive Adaptive preview cadence (1000 px / 100 ms) and 4×-core pool (cap 32) | Tuned back to ~4000 / 250 ms and 2× cores cap 16 (`7c32fe3`). |

## Known gaps left open (intentionally)

| Issue | Topic |
|-------|--------|
| [#47](https://github.com/girino/android-mandelbrot-set/issues/47) | Experimental: remap palette from **observed** iter min/max |
| [#48](https://github.com/girino/android-mandelbrot-set/issues/48) | Experimental: do not publish all-black progressive frames in Adaptive |
| [#38](https://github.com/girino/android-mandelbrot-set/issues/38)–[#45](https://github.com/girino/android-mandelbrot-set/issues/45) | Deferred heavier opts (scalar rest, OrbitState RAM, smooth log(k), bulbs, periodicity, Mariani–Silver, deep zoom, GPU/JNI) |
| [#19](https://github.com/girino/android-mandelbrot-set/issues/19), [#20](https://github.com/girino/android-mandelbrot-set/issues/20) | Persist session across process death; accessibility |

Deep zoom still hits “everything black until Adaptive finds an edge” — that is
exactly what #48 experiments on. Do not “fix” it by publishing coarse black
frames or by breaking deferred-commit viewport rules.

## Lessons

1. **Measure on device before merging Adaptive “optimizations”.** #35 looked
   good on paper and lost on silicon.
2. **GC wins ≠ FPS wins.** #33 is fine to keep; do not sell it as a speedup.
3. **Adaptive UX ≠ Adaptive math.** Stop-floor, perimeter seed, and preview
   publish policy matter as much as doubling. Several Adaptive commits were
   pure product semantics after the core loop worked.
4. **Never touch gesture publish gates casually.** Progressive + Adaptive still
   must honor `activePointers` and atomic handoff
   ([viewport-smooth-transition.mdc](../.cursor/rules/viewport-smooth-transition.mdc)).
5. **Agent-facing docs ≠ README.** Zapstore/CI/secrets belong in `docs/` and
   `.cursor/rules/`, not the README.
6. **Bash + bunker URI:** always quote `SIGN_WITH`; never embed bunker in
   `wsl bash -lc`.

## Related documents

| Doc | Role |
|-----|------|
| [CHANGELOG.md](../CHANGELOG.md) § 1.1.0 | User-facing bullet list |
| [POSTMORTEM-adaptive-iteration.md](POSTMORTEM-adaptive-iteration.md) | Adaptive failed/reverted paths |
| [HANDOFF.md](HANDOFF.md) | Next-agent checklist |
| [POSTMORTEM-viewport-flicker.md](POSTMORTEM-viewport-flicker.md) | Still the law for `MandelbrotView` |
| [DEPLOY.md](DEPLOY.md) | Tag → CI → Zapstore |
