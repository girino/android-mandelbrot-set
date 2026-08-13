# Fractals by Girino

An interactive Android fractal explorer. Pan and pinch to navigate, switch between
fractal formulas and palettes, and enable smooth coloring.

The app is written in Java and uses only Android platform APIs. It contains no ads,
trackers, network access, analytics, or proprietary runtime dependencies.

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

## License

This project is distributed under the custom
[Girino Anarchist's License](LICENSE). The canonical license text is also
available at [license.girino.org](https://license.girino.org/).
