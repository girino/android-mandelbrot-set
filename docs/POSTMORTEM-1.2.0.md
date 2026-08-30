# Postmortem — v1.2.0 release

Retrospective for the **1.2.0** stable line (Aug 2026). Not a bug postmortem —
release planning and triage notes for the next agent.

Previous baseline: [POSTMORTEM-1.1.0.md](POSTMORTEM-1.1.0.md). User-facing
history: [CHANGELOG.md](../CHANGELOG.md).

---

## What shipped

| Area | Highlights |
|------|------------|
| Catalog | +5 formulas (Phoenix, Julia, Julia Phoenix, Celtic, Perpendicular); Tricorn rename; +7 palettes |
| Session | Cold-start restore (#19) via `SessionStore` |
| Iteration | Defaults 64 / cap 2^18 / 18 rounds; settings decoupled from HUD Reset |
| Rendering | #51 palette exterior fix; #48 Adaptive black-skip; #40 log(k) smooth; Shipbar fix |
| Gestures | Chained pinch during frozen preview; resume skip when frame ready |
| CI | `backendTestDebugUnitTest` only; cancel/lifecycle test fixes |

Tag: **v1.2.0** (`versionCode` 11). Supersedes the unpublished **1.1.1-alpha**
pre-release (same iteration defaults were already in that alpha line).

---

## Experiments closed this cycle

| Issue | Outcome |
|-------|---------|
| #50 | Iteration-step progressive and 32→…→1 spatial extension — no on-device win |
| #47 | Adaptive palette from observed iter range — won't do |
| #38 | Scalar steps for remaining ops — won't do (no evident gain) |
| #44 | Deep-zoom perturbation / multiprecision — won't do (niche, huge architecture) |
| #20 | Full accessibility pass — won't do (gesture-first canvas) |

**Result:** zero open GitHub issues after v1.2.0 triage.

---

## CI lesson (carried from 1.1.1 work)

Robolectric gesture tests are valuable **locally** but were removed from CI
(`backendTestDebugUnitTest`) after repeated hangs/timeouts. Always run
`MandelbrotViewGestureTest` on a dev machine before merging gesture changes;
validate on device for any `MandelbrotView` edit.

---

## Release checklist used

1. `versionCode` / `versionName` + `CHANGELOG.md`
2. Doc pass: `HANDOFF.md`, `USAGE.md`, `AGENTS.md`, Fastlane changelogs
3. Local: `lintRelease backendTestDebugUnitTest`
4. Commit → tag `v1.2.0` → push → GitHub Actions signed APK
5. Optional: Zapstore via WSL + `.credentials.env` ([DEPLOY.md](DEPLOY.md))

---

## Suggested focus if the product continues

No tracked backlog. Likely directions only if the maintainer asks:

- New formulas / palettes (low risk, catalog pattern exists)
- Performance on specific devices (profile first)
- Re-open deep zoom or accessibility only with a clear product goal

Do **not** retry #50 approaches without new evidence.
