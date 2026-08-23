# Usage guide

How to use Fractals by Girino FOSS on Android.

## Installing

Two channels distribute the same app (`org.girino.frac.android.foss`):

| Channel | How | Updates |
|---------|-----|---------|
| **GitHub Releases** (recommended) | Install the APK from the [latest release](https://github.com/girino/android-mandelbrot-set/releases/latest), or add the repo to [Obtainium](https://github.com/ImranR98/Obtainium): `obtainium://add?url=https%3A%2F%2Fgithub.com%2Fgirino%2Fandroid-mandelbrot-set` | Automatic via Obtainium; manual otherwise |
| **Zapstore** | Search "Fractals by Girino" in Zapstore, or install via `zsp`-published releases | In-app update check |

The GitHub and Zapstore APKs share one signing key and can replace each other.

## Navigating the fractal

- **Pan**: drag with one finger. The image follows your finger 1:1 while a live
  preview moves it; the high-resolution render catches up after you lift.
- **Pinch zoom**: pinch with two fingers. Zoom is anchored on the midpoint
  between your fingers (not the screen center), so the fractal under your
  fingers stays put while you zoom or drag the pinch. Zoom commits when you
  lift the *last* finger — bringing fingers close together does not end the
  gesture early.
- **Double-tap**: tap twice quickly to zoom in about the point you tapped.
- **Long-press**: hold to show the complex coordinates under your finger;
  lift to dismiss.
- **HUD − / +**: zoom out or in about the screen center without pinching.
- During any gesture the picture stays smooth: the screen shows a transformed
  preview and never flashes an older bitmap. Brief gaps at the edges during
  a large pinch preview are normal; the new render fills the whole screen.

## Bottom controls (HUD)

A compact bar at the bottom of the screen:

| Control | Action |
|---------|--------|
| **Formula** | Open the formula bottom sheet (current formula marked) |
| **Palette** | Open the palette bottom sheet (current palette marked; color swatch per row) |
| **−** / **+** | Zoom out / zoom in about the screen center |
| **Smooth** | Toggle continuous iteration coloring (stays pressed when on) |
| **Reset** | Reset viewport to the initial position and scale |

Pan and pinch still work on the fractal above the bar; the bar does not
start a drag.

While a progressive render is running after you lift your finger (or after
zoom / reset / palette change), a thin progress bar at the top fills in
proportion to completed samples (steps 8→4→2→1, weighted by work). It
disappears when the full-resolution frame is ready or when a new gesture
cancels the render.

There is no options menu (Material NoActionBar + HUD). Leave the app with
the system Back gesture or Recents. The fractal draws edge-to-edge under the
status and navigation bars; the HUD stays above the nav bar.

## Privacy

The app has no network access, no ads, no trackers, no analytics. Everything
renders locally on your device. See [PRIVACY.md](PRIVACY.md).
