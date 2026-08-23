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
- **Double-tap**: tap twice quickly to zoom in about the point you tapped.
- **HUD − / +**: zoom out or in about the screen center without pinching.
- During any gesture the picture stays smooth: the screen shows a transformed
  preview and never flashes an older bitmap. Brief gaps at the edges during
  a large pinch preview are normal; the new render fills the whole screen.

## Bottom controls (HUD)

A compact bar at the bottom of the screen:

| Control | Action |
|---------|--------|
| **Formula** | Open the formula list |
| **Palette** | Open the palette list |
| **−** / **+** | Zoom out / zoom in about the screen center |
| **Smooth** | Toggle continuous iteration coloring (stays pressed when on) |
| **Reset** | Reset viewport to the initial position and scale |

Pan and pinch still work on the fractal above the bar; the bar does not
start a drag.

While a progressive render is running after you lift your finger (or after
zoom / reset / palette change), a short indeterminate bar appears at the top
of the screen. It disappears when the full-resolution frame is ready or when
a new gesture cancels the render.

## Overflow menu

Open the options menu for the same actions plus:

| Item | Action |
|------|--------|
| **Formula** / **Palette** / **Smooth** / **Reset** | Same as the HUD |
| **Zoom in** / **Zoom out** | Same as HUD + / − |
| **Exit** | Close the app |


## Privacy

The app has no network access, no ads, no trackers, no analytics. Everything
renders locally on your device. See [PRIVACY.md](PRIVACY.md).
