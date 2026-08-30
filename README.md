# Fractals by Girino FOSS

An interactive Android fractal explorer. Pan, pinch, and double-tap to navigate,
long-press for coordinates, choose among **12 formulas** and **12 palettes** from
a bottom HUD, export the viewport as PNG, toggle smooth coloring from the menu,
and pick fixed / scale-with-zoom / **adaptive** iteration limits. Your last
viewport and choices are restored after the app is closed. The fractal draws
edge-to-edge under the system bars.

This FOSS edition uses the application ID `org.girino.frac.android.foss`, so it
can be installed alongside the historical Google Play edition without conflicts.

The app is written in Java. UI uses AndroidX AppCompat and Material Components
for theming and bottom-sheet pickers; fractal math and rendering stay on platform
graphics APIs. It contains no ads, trackers, network access, analytics, or
proprietary runtime dependencies.

## Install with Obtainium

On Android, open this Obtainium link:

`obtainium://add?url=https%3A%2F%2Fgithub.com%2Fgirino%2Fandroid-mandelbrot-set`

If the link does not open automatically:

1. Open **Add App** in Obtainium.
2. Enter `https://github.com/girino/android-mandelbrot-set` as the app source URL.
3. Leave **Include prereleases** disabled to receive only stable versions.
4. Enable **Include prereleases** only if you want alpha, beta, or other test builds.
5. Confirm the detected APK and install it.

The GitHub and Zapstore APKs share one signing key and can replace each other
as long as that signing key is preserved.

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

Signed GitHub Releases and Zapstore publishing: [docs/DEPLOY.md](docs/DEPLOY.md).

## License

This project is licensed under the **Girino Anarchist License (GAL)** only.
The complete text is in [LICENSE](LICENSE) (same as [LICENSE.GAL](LICENSE.GAL))
and at [license.girino.org](https://license.girino.org/).

Distribution is via **GitHub Releases** and **Zapstore** only. The project is
no longer prepared for or published through the F-Droid repository. Releases
before this policy change may have been dual-licensed under BSD 2-Clause for
F-Droid; new releases are GAL-only.
