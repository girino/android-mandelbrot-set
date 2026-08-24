# Deployment guide

How releases are built, signed, and published to GitHub Releases and Zapstore.

## Overview

There is no manual APK distribution. Everything flows from a **git tag**:

```
git tag vX.Y.Z  ->  push tag  ->  GitHub Actions "Android release" workflow
                                     |  test + lint + signed build
                                     v
                              GitHub Release (stable or pre-release)
                                     |
                                     v  (manual, on demand)
                              Zapstore via zsp from WSL
```

## Prerequisites (one-time)

### GitHub Actions secrets

The workflow signs the APK with these repository secrets
(Settings → Secrets and variables → Actions):

| Secret | Content |
|--------|---------|
| `ANDROID_KEYSTORE_BASE64` | release keystore (`base64 -w0 release.jks`) |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | signing key alias |
| `ANDROID_KEY_PASSWORD` | key password |

Keep the historical signing key: existing installations only accept updates
signed with the same key.

Check secrets exist:

```powershell
gh secret list -R girino/android-mandelbrot-set
```

### Zapstore tooling

`zsp` does not build on Windows; publish from WSL (one-time setup):

```bash
go install github.com/zapstore/zsp@latest   # Go inside WSL
export PATH="$HOME/go/bin:$PATH"
zsp --version
```

If `go` fails with `cannot create XDG_RUNTIME_DIR "/run/user/1000"`, fix once
as root in WSL:

```bash
mkdir -p /run/user/1000 && chown 1000:1000 /run/user/1000
```

Signing uses `SIGN_WITH` (`bunker://...` or nsec) from the gitignored
[`.credentials.env`](../.credentials.env) at the repo root, in dotenv form
**with the value quoted** (required — `&` in the URI breaks unquoted `source`):

```text
SIGN_WITH='bunker://...?relay=wss://...&secret=...'
```

Never commit that file, never print `SIGN_WITH` in chat/logs. Agent load/WSL
gotchas: [`.cursor/rules/zapstore-wsl.mdc`](../.cursor/rules/zapstore-wsl.mdc).

## Cutting a stable release

1. **Prepare the tree**
   - Bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`.
   - Add the release section at the top of `CHANGELOG.md`.
2. **Validate locally** (lint failure blocks CI):

   ```powershell
   $env:JAVA_HOME = Join-Path $PWD ".jdk"
   $env:ANDROID_HOME = Join-Path $PWD ".android-sdk"
   .\gradlew.bat lintRelease testDebugUnitTest assembleRelease
   ```

3. **Commit, tag, push** (push requires explicit user request per repo policy;
   a release request covers its own tag push):

   ```powershell
   git add app/build.gradle.kts CHANGELOG.md
   git commit -m "Release X.Y.Z."
   git tag vX.Y.Z
   git push origin master
   git push origin vX.Y.Z
   ```

4. **Watch CI** — the *Android release* run builds `testDebugUnitTest
   lintRelease assembleRelease`, copies `app-release.apk` to
   `Fractals-by-Girino-FOSS-<tag>.apk` (+ `.sha256`), and creates the release.
   Tags matching exactly `vMAJOR.MINOR.PATCH` publish as **stable**; any other
   tag publishes as **pre-release**.

   ```powershell
   gh run watch <run-id> -R girino/android-mandelbrot-set --exit-status
   gh release view vX.Y.Z -R girino/android-mandelbrot-set
   ```

5. **Publish to Zapstore** (on request). From WSL, `source` `.credentials.env`
   (quoted `SIGN_WITH=...`), then use an **absolute** config path — see
   `.cursor/rules/zapstore-wsl.mdc` (do not embed the bunker in `bash -lc`):

   ```bash
   REPO=/mnt/f/cygwin64/home/girino/git/android-mandelbrot-set
   set -a && . "$REPO/.credentials.env" && set +a
   CFG="$REPO/zapstore.yaml"
   zsp publish --check "$CFG"
   zsp publish -q "$CFG"
   zsp utils has-new-release "$CFG"
   # {"has_new_release":false,"release_version":"X.Y.Z"} confirms publication
   ```

Full validated walkthrough: `.cursor/rules/zapstore-wsl.mdc`.

## Pre-releases

Identical flow; use a suffix tag such as `v1.1.0-alpha`. The workflow marks it
`prerelease: true`, so Obtainium users tracking stables do not receive it.

## Troubleshooting

| Symptom | Cause / fix |
|---------|-------------|
| CI fails in `lintAnalyzeRelease` with `JavaDocParser.parseDataItem` NoSuchMethodError | Inline JDoc tags (`{@code}`, `{@link}`) in `app/src/main/**`. Remove them; see DEVELOPMENT.md lint section. |
| Release missing / wrong asset name | Check the workflow's "Prepare release assets" step output and the tag format rule above. |
| Push opens credential dialogs | Token expired or orphaned git processes — see `.cursor/rules/git-credentials-github.mdc`; never accept the GUI dialog blindly. |
| `zsp` says "failed to open config file" though the file exists | Piped stdin breaks relative paths; pass an absolute path. |
| `SIGN_WITH environment variable is required` / bash syntax error around bunker | Empty var, bunker in `bash -lc`, or unquoted `SIGN_WITH=` (`&` cuts URI on source); quote the value — see zapstore-wsl.mdc. |
