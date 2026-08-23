# Usage guide

How to use Fractals by Girino FOSS on Android.

## Installing

Three channels distribute the same app (`org.girino.frac.android.foss`):

| Channel | How | Updates |
|---------|-----|---------|
| **GitHub Releases** (recommended) | Install the APK from the [latest release](https://github.com/girino/android-mandelbrot-set/releases/latest), or add the repo to [Obtainium](https://github.com/ImranR98/Obtainium): `obtainium://add?url=https%3A%2F%2Fgithub.com%2Fgirino%2Fandroid-mandelbrot-set` | Automatic via Obtainium; manual otherwise |
| **Zapstore** | Search "Fractals by Girino" in Zapstore, or install via `zsp`-published releases | In-app update check |
| **F-Droid** (official repo) | See [F-DROID.md](F-DROID.md) | Via F-Droid client |

The GitHub and Zapstore APKs share one signing key and can replace each other.
The F-Droid build is signed by F-Droid, so switching between F-Droid and
GitHub/Zapstore builds requires uninstalling first. Details in
[README.md](README.md#license).

## Navigating the fractal

- **Pan**: drag with one finger. The image follows your finger 1:1 while a live
  preview moves it; the high-resolution render catches up after you lift.
- **Pinch zoom**: pinch with two fingers. Zoom is anchored on the midpoint
  between your fingers (not the screen center), so the fractal under your
  fingers stays put while you zoom or drag the pinch. Zoom commits when you
  lift the *last* finger — bringing fingers close together does not end the
  gesture early.
- During any gesture the picture stays smooth: the screen shows a transformed
  preview and never flashes an older bitmap. Brief gaps at the edges during
  a large pinch preview are normal; the new render fills the whole screen.

## Menu

Open the options menu for:

| Item | Action |
|------|--------|
| **Formula** | Pick a fractal: Mandelbrot, Optimized Mandelbrot, Julia, Nova, Burning Ship, Mandelbar, ShipBar, cube/fourth/fifth-power variants, WTF, and test operators |
| **Palette** | Pick a color palette (default, red/green/blue variants, HSB, smooth fixed) |
| **Smooth palette** | Toggle continuous (smooth) iteration coloring; a checkmark shows when it is on |
| **Zoom** | Reset to a comfortable default zoom |
| **Reset** | Reset viewport to the initial position and scale |
| **Exit** | Close the app |

## Privacy

The app has no network access, no ads, no trackers, no analytics. Everything
renders locally on your device. See [PRIVACY.md](PRIVACY.md).
