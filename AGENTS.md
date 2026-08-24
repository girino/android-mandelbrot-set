# AGENTS.md

Guidance for AI coding agents (OpenCode, Claude Code, Cursor, Codex, etc.)
working in this repository. Humans: see [README.md](README.md) and
[docs/](docs/).

## Repository rules (read first — mandatory)

The source of truth for agent behavior lives in **`.cursor/rules/*.mdc`**.
They apply to *any* agent working here, not only Cursor. Frontmatter
(`alwaysApply`, `globs`) works like in Cursor; when a rule matches your task,
follow it.

| Rule file | Applies | Summary |
|-----------|---------|---------|
| [`.cursor/rules/git-commit-policy.mdc`](.cursor/rules/git-commit-policy.mdc) | always | Commit after finished work; push (and tag/release) only on explicit user request |
| [`.cursor/rules/git-credentials-github.mdc`](.cursor/rules/git-credentials-github.mdc) | always | Silent auth via `gh` credential helper; if an auth dialog appears, diagnose (expired token / orphaned git processes), never accept the GUI |
| [`.cursor/rules/local-android-sdk.mdc`](.cursor/rules/local-android-sdk.mdc) | always | Use repo-local `.jdk/` and `.android-sdk/`; never global SDK/JDK; no JDoc inline tags in `app/src/main/**` (lint crash) |
| [`.cursor/rules/viewport-smooth-transition.mdc`](.cursor/rules/viewport-smooth-transition.mdc) | always | Mandatory smooth pan/pinch model: deferred commit + atomic handoff in `MandelbrotView` — read before touching it |
| [`.cursor/rules/headless-gesture-tests.mdc`](.cursor/rules/headless-gesture-tests.mdc) | gesture/view files | Robolectric headless tests for pinch+drag; run `MandelbrotViewGestureTest` after gesture changes |
| [`.cursor/rules/zapstore-wsl.mdc`](.cursor/rules/zapstore-wsl.mdc) | `zapstore.yaml` / release publishing | Zapstore via WSL; `.credentials.env` with **quoted** `SIGN_WITH='bunker://...?&...'` (unquoted `&` breaks `source`) |

Conflict order observed in this repo: explicit user request > these rules >
your own defaults.

## Project snapshot

- Android app, pure Java. UI uses AppCompat + Material Components (HUD,
  bottom-sheet pickers, edge-to-edge); fractal rendering stays on platform
  graphics. Application ID `org.girino.frac.android.foss`.
- **Current release line:** 1.1.0 (see `app/build.gradle.kts` and
  [CHANGELOG.md](CHANGELOG.md)).
- Build/test/lint commands and repo-local toolchain: [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).
- Release/deploy flow (tag → CI signed APK → GitHub Release → Zapstore):
  [docs/DEPLOY.md](docs/DEPLOY.md).
- User-facing behavior: [docs/USAGE.md](docs/USAGE.md).
- Adaptive iteration algorithm: [docs/ADAPTIVE-ITERATION.md](docs/ADAPTIVE-ITERATION.md).

## Working conventions

1. **Do not change code when the user asks a question** — answer first,
   implement only on request.
2. **Commits**: concise message, related files only, no build junk. Push only
   when asked (release requests include their own tag push).
3. **Secrets**: never print tokens/keystore passwords/bunker URIs into chat or
   logs. Env vars per-process only.
4. **PowerShell quirks**: no `&&` separator on this machine's Windows
   PowerShell 5.1 — use `;` or separate calls. No heredoc (`<<'EOF'`) for
   commit messages — use multiple `-m` flags.
5. **Lint trap**: `{@code}`/`{@link}` tags crash lint's JavaDoc parser here.
   Plain text only.
6. **Before tagging a release**: run `lintRelease testDebugUnitTest`
   locally with repo-local SDK/JDK.

## The viewport story (why the code looks like it does)

The main risk area is `app/src/main/java/org/girino/frac/android/foss/MandelbrotView.java`.
Six flicker-fix attempts failed before the current model landed (v1.0.2).
Read [docs/POSTMORTEM-viewport-flicker.md](docs/POSTMORTEM-viewport-flicker.md)
— it lists what was tried and must not be repeated — and honor
`.cursor/rules/viewport-smooth-transition.mdc` (deferred commit, commit only
on last-pointer-up, atomic bitmap+preview swap). Run
`MandelbrotViewGestureTest` and validate on-device for any gesture change.

## Adaptive render (issues #28 / #31)

After progressive step 1 in **Adaptive** mode, `AdaptiveRefiner` doubles
iteration limits on interior border pixels only. Key implementation points:

| Piece | Role |
|-------|------|
| `OrbitState` | Warm-start checkpoints from pass-1 (`sampleContinue` / `sampleContinueInto`) |
| `EscapeSample` | Mutable worker-local result via `sampleInto` (issue #33; see revert notes on the class) |
| `workerPool` | Progressive steps 8→4→2→1 — `min(8, cores)` |
| `adaptiveWorkerPool` | Parallel border collect + retest — `min(16, 2× cores)` |
| `PreviewListener` | Throttled in-progress bitmap swap (~4000 px / 250 ms) |
| `visitedAtLimit` | Per-limit bitmap: skip border pixels already probed until cap doubles |
| Indeterminate bar | After step 1 until refine completes (issue #31) |

**Do not** publish bitmaps or start renders while `activePointers > 0`.
**Do not** remap the full palette on each border round — only retested pixels
change color. Overlay **Iter** (`adaptiveMaxIter`) is the stop-floor for the
next zoom's refine.

Tuning and algorithm details: [docs/ADAPTIVE-ITERATION.md](docs/ADAPTIVE-ITERATION.md).
Tests: `AdaptiveRefinerTest`, `FractalOperatorContinueTest`.

## Historical postmortems

| Document | Topic |
|----------|-------|
| [docs/POSTMORTEM-viewport-gestures.md](docs/POSTMORTEM-viewport-gestures.md) | Failed approaches before v1.0.2-alpha2 (handoff, bridge, manual pinch, publish gates) |
| [docs/POSTMORTEM-viewport-flicker.md](docs/POSTMORTEM-viewport-flicker.md) | The fix that worked: deferred commit + atomic handoff (v1.0.2) |
| [docs/POSTMORTEM-pinch-anchor.md](docs/POSTMORTEM-pinch-anchor.md) | Focus-anchored pinch (issue #3) — failed attempts and v1.0.3 fix |
