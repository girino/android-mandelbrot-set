# Agent handoff — post v1.2.0

Onboarding for a **new coding agent** continuing this repo after the v1.2.0
release. Humans who only use the app: see [README.md](../README.md) and
[USAGE.md](USAGE.md). Maintainer release ops: [DEPLOY.md](DEPLOY.md).

Read order (mandatory before editing risk areas):

1. [AGENTS.md](../AGENTS.md) + every matching `.cursor/rules/*.mdc`
2. This file
3. [POSTMORTEM-1.2.0.md](POSTMORTEM-1.2.0.md) (latest release notes)
4. [POSTMORTEM-1.1.0.md](POSTMORTEM-1.1.0.md) (v1.1.0 retrospective)
5. If touching Adaptive: [ADAPTIVE-ITERATION.md](ADAPTIVE-ITERATION.md) +
   [POSTMORTEM-adaptive-iteration.md](POSTMORTEM-adaptive-iteration.md)
6. If touching gestures / `MandelbrotView`:
   [viewport-smooth-transition.mdc](../.cursor/rules/viewport-smooth-transition.mdc)
   + [POSTMORTEM-viewport-flicker.md](POSTMORTEM-viewport-flicker.md)

---

## Current product state

| Field | Value |
|-------|-------|
| Release line | **1.2.0** (`app/build.gradle.kts`, tag `v1.2.0`) |
| App ID | `org.girino.frac.android.foss` |
| Language | Pure Java (AppCompat / Material UI; platform graphics for fractal) |
| Formulas | 12 (Mandelbrot, Burning Ship, Nova, Tricorn, Cube, Fourth, Shipbar, Phoenix, Julia, Julia Phoenix, Celtic, Perpendicular) |
| Palettes | 12 (Green, Blue, Red, RGB, BGR, Fire, Ocean, Grayscale, Sunset, Neon, Viridis, Electric) |
| Channels | GitHub Releases + Zapstore (no F-Droid) |
| License | GAL only |
| Open GitHub issues | **None** (backlog triaged Aug 2026) |

## Machine / toolchain (this maintainer’s setup)

- OS: Windows + PowerShell 5.1 (**no `&&`**; use `;`). Commit messages: multiple
  `-m` flags (no bash heredoc).
- **Always** use repo-local `.jdk/` and `.android-sdk/` — never the system SDK
  ([local-android-sdk.mdc](../.cursor/rules/local-android-sdk.mdc)).
- Headless gesture tests (local): `.\scripts\setup-headless-tests.ps1 -RunTests` or
  `.\gradlew.bat testDebugUnitTest`. CI uses `backendTestDebugUnitTest` only.
- Before any release tag: `lintRelease backendTestDebugUnitTest` locally.
- Lint trap: **no** `{@code}` / `{@link}` in `app/src/main/**` comments.
- Git push: only when the user asks; silent `gh` credential helper
  ([git-credentials-github.mdc](../.cursor/rules/git-credentials-github.mdc)).
- Zapstore: WSL only; `.credentials.env` with **quoted** `SIGN_WITH=...`
  ([zapstore-wsl.mdc](../.cursor/rules/zapstore-wsl.mdc)). Never print the
  bunker URI.

## Architecture map (where to edit)

| Concern | Primary files |
|---------|----------------|
| Gestures / publish / progress | `MandelbrotView.java` |
| Progressive fill | `ParallelStepRenderer.java` |
| Adaptive refine | `AdaptiveRefiner.java`, `OrbitState.java` |
| Escape math | `operators/FractalOperator.java` + catalog operators |
| Palettes | `palettes/*`, `PaletteLut.java` |
| HUD / menus | `MandelbrotActivity.java`, `activity_mandelbrot.xml`, `HudMenuAdapter.java` |
| Iteration modes | `IterationSettings*.java` |
| Session (rotate + cold start) | `ViewportSession.java`, `SessionStore.java` |
| Julia / Phoenix params | `FormulaParamsActivity.java`, `*ParamsStore.java` |

## Invariants (break these → user-visible regressions)

1. **Deferred commit + atomic handoff** — no render start / no bitmap publish
   while `activePointers > 0`; commit on last pointer up; swap bitmap + clear
   preview + reset focus in one `post()`.
2. Adaptive: **no full-frame palette remap** each border round.
3. Adaptive: pass-1 max stays at Fixed setting; overlay Iter is stop-floor /
   display, not the next pass-1 budget.
4. README stays **end-user only** ([readme-user-facing.mdc](../.cursor/rules/readme-user-facing.mdc)).

## If new work is requested

There is no open issue backlog. New features need a fresh GitHub issue or
explicit user direction. Closed as **won't do** in v1.2.0 cycle: accessibility
(#20), scalar remaining ops (#38), deep-zoom perturbation (#44), adaptive
palette (#47), iteration-step / extended progressive (#50), plus earlier
research items (#41–#43, #45, #49).

### Do not reopen without evidence

[#35](https://github.com/girino/android-mandelbrot-set/issues/35) incremental
`collectBorder` — slower on device.

## Validation checklist (any non-trivial change)

```powershell
$env:JAVA_HOME = Join-Path $PWD ".jdk"
$env:ANDROID_HOME = Join-Path $PWD ".android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
.\gradlew.bat lintRelease backendTestDebugUnitTest
```

- Gesture / `MandelbrotView`: also run `MandelbrotViewGestureTest` locally and
  **on-device** pan, pinch, zoom-out with fingers closing, chained gestures
  before render ends.
- Adaptive: on-device deep zoom + Adaptive mode; black-flash skip (#48) and
  stop-floor behavior.
- Never paste secrets into chat or commits.

## Communication / process quirks

- Answer questions without coding until the user asks to implement
  (`AGENTS.md`).
- Project rule: commit finished work; push only on explicit request (release
  requests include tag push).
- Prefer Portuguese (BR) in chat with this maintainer.

## Document index

| Doc | Audience |
|-----|----------|
| [README.md](../README.md) | End users |
| [USAGE.md](USAGE.md) | End-user behavior detail |
| [DEVELOPMENT.md](DEVELOPMENT.md) | Build / test / layout |
| [DEPLOY.md](DEPLOY.md) | Release + Zapstore |
| [CHANGELOG.md](../CHANGELOG.md) | Version history |
| [POSTMORTEM-1.2.0.md](POSTMORTEM-1.2.0.md) | v1.2.0 release retrospective |
| [POSTMORTEM-1.1.0.md](POSTMORTEM-1.1.0.md) | v1.1.0 retrospective |
| [POSTMORTEM-adaptive-iteration.md](POSTMORTEM-adaptive-iteration.md) | Adaptive wrong turns |
| Viewport POSTMORTEM-* | Gesture flicker / pinch (still binding for MandelbrotView) |
