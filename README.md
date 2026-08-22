# Fractals by Girino FOSS

An interactive Android fractal explorer. Pan and pinch to navigate, switch between
fractal formulas and palettes, and enable smooth coloring.

This FOSS edition uses the application ID `org.girino.frac.android.foss`, so it
can be installed alongside the historical Google Play edition without conflicts.

The app is written in Java and uses only Android platform APIs. It contains no ads,
trackers, network access, analytics, or proprietary runtime dependencies.

## Install with Obtainium

On Android, open this Obtainium link:

`obtainium://add?url=https%3A%2F%2Fgithub.com%2Fgirino%2Fandroid-mandelbrot-set`

If the link does not open automatically:

1. Open **Add App** in Obtainium.
2. Enter `https://github.com/girino/android-mandelbrot-set` as the app source URL.
3. Leave **Include prereleases** disabled to receive only stable versions.
4. Enable **Include prereleases** only if you want alpha, beta, or other test builds.
5. Confirm the detected APK and install it.

The GitHub and Zapstore APKs are signed with the upstream author's key and can
update each other as long as that signing key is preserved. The official F-Droid
build uses F-Droid's signing key, so an F-Droid installation cannot be updated
with the GitHub APK (or vice versa) without uninstalling the existing app first.

## Build

Requirements:

- JDK 17 or newer
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0

Set `sdk.dir` in an untracked `local.properties` file or export
`ANDROID_SDK_ROOT`, then run:

```shell
./gradlew assembleDebug lintDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

Preparation and submission instructions for the official F-Droid repository
are documented in [F-DROID.md](F-DROID.md).

## GitHub releases

Every pushed tag builds a signed release APK and publishes it on the GitHub
Releases page. Tags that exactly match `vMAJOR.MINOR.PATCH`, such as `v1.3.0`,
create a final release. Every other tag creates a pre-release.

Configure these GitHub Actions repository secrets before pushing a tag:

- `ANDROID_KEYSTORE_BASE64`: the release keystore encoded with Base64
- `ANDROID_KEYSTORE_PASSWORD`: the keystore password
- `ANDROID_KEY_ALIAS`: the signing key alias
- `ANDROID_KEY_PASSWORD`: the signing key password

Keep the historical signing key if releases must update existing installations.

## Zapstore

Stable GitHub releases can be published to [Zapstore](https://zapstore.dev) with
[`zsp`](https://github.com/zapstore/zsp). The repo root contains
[`zapstore.yaml`](zapstore.yaml) with the publisher npub and Fastlane metadata
sources.

Publish manually from WSL (see [`.cursor/rules/zapstore-wsl.mdc`](.cursor/rules/zapstore-wsl.mdc)):

```shell
export SIGN_WITH='bunker://...'   # or nsec — use your local env, not GitHub secrets
export GITHUB_TOKEN='...'         # if release fetch requires a token
zsp publish -q zapstore.yaml
```

On the first publish, `zsp` may prompt to link the Android signing certificate
to your Nostr identity.

## License

Girino Anarchist's License (GAL) remains the author's preferred license for
this project. Its complete original text is preserved in
[LICENSE.GAL](LICENSE.GAL) and at
[license.girino.org](https://license.girino.org/).

The project is also made available under the
[BSD 2-Clause License](LICENSE). This additional license was adopted solely to
satisfy the free-software licensing requirements of F-Droid and enable
distribution through its repositories. Recipients may use the project under
either the BSD 2-Clause License or the GAL, at their option.

The BSD grant cannot legally be restricted only to copies downloaded from
F-Droid: the F-Droid build must be produced from freely licensed source, and
the rights granted by the BSD 2-Clause License accompany redistributed source
and binaries. Outside that requirement, the author asks users to choose and
honor the GAL whenever possible.
