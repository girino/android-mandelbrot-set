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

A compact bar at the bottom of the screen (icons only):

| Icon | Action |
|------|--------|
| **+** | Zoom in about the screen center |
| **−** | Zoom out about the screen center |
| **↻** | Reset viewport to the initial position and scale; also restores iteration settings to defaults (fixed 40) |
| **⇪** | Export viewport as PNG (share or save to gallery) |
| **☰** | Open the menu (all actions with icon + name) |

The **menu** lists zoom in/out, reset, formula, palette, then **More**:
**Smooth** (with on/off), **Iterations**, **Help**, and **About**. Formula and palette
open bottom sheets: formula rows show a mini fractal thumbnail; palette rows show
a color swatch. Smooth toggles continuous iteration coloring. Iterations opens
a settings screen for escape-time max iterations. Help summarizes gestures and
the HUD; About shows the version, package id, and **Girino Anarchist License
(GAL)** with a link to the full license text.

## Iterations

Menu → **Iterations** chooses how escape-time max iterations are computed:

| Mode | Fields | Behavior |
|------|--------|----------|
| **Fixed max** (default) | Max iterations (default **40**) | Same cap for every pixel at every zoom |
| **Scale with zoom** | Base (default **40**) and multiplier (default **1.2**) | At home zoom, maxIter = base. Each doubling of zoom multiplies by the multiplier: `round(base × multiplier^log2(scale/homeScale))`, clamped to 10–1048576 (soft UI warning above 4096) |
| **Adaptive** | Pass-1 max (same Fixed field, default **40**), max rounds (default **8**), absolute cap (default **4096**, hard max **1048576**) | Full progressive pass at pass-1 max, then only re-tests interior border pixels (4-connected to an escaped neighbor). At each doubled limit, keeps retesting newly exposed borders until a pass finds no new escapes, then doubles again (or stops if that first pass found nothing). Intermediate fills are painted on screen. The status overlay **Iter** shows the max from the last border round; zoom does not raise pass-1 from that value. Tip: lower pass-1 (e.g. 10–20) makes the first-frame border growth easier to see. |

Settings survive process restart via SharedPreferences. Changing them re-renders
(respecting the gesture gate). The status overlay shows the effective **Iter**
value for the current viewport (Fixed / zoom-resolved base; Adaptive shows the
last border-round limit when available).

See [docs/ADAPTIVE-ITERATION.md](ADAPTIVE-ITERATION.md) for algorithm notes and
why Mariani–Silver was not chosen for v1.

Pan and pinch still work on the fractal above the bar; the bar does not
start a drag.

Rotating the device keeps the same fractal region (viewport, formula, palette,
and smooth setting). A full **Reset** still returns to the canonical initial view.

While a progressive render is running after you lift your finger (or after
zoom / reset / palette change), a thin progress bar at the top fills in
proportion to completed samples (steps 8→4→2→1, weighted by work). In
**Adaptive** mode, after step 1 the bar switches to an indeterminate
animation while border refine runs; the fractal image may update every
~4000 border pixels or ~250 ms as the refine progresses. It disappears when
the full-resolution frame is ready or when a new gesture cancels the render.
Progressive step 1 uses up to eight worker threads; Adaptive border collect
and retest use a separate pool (up to sixteen threads on typical phones).
While the limit stays fixed, already-probed border pixels are not retested
until the cap doubles. Pan/pinch preview and handoff stay the same.

There is no options menu (Material NoActionBar + HUD). Leave the app with
the system Back gesture or Recents. The fractal draws edge-to-edge under the
status and navigation bars; the HUD stays above the nav bar.

A small overlay in the top-left corner shows the current formula, palette,
smooth coloring, iteration algorithm (Fixed / Scale with zoom / Adaptive), and
effective iteration cap. Tap the overlay to hide it; tap
the **···** chip in the same corner to show it again. Pan and pinch on the
fractal are unchanged. The HUD bar uses a similar translucent background.

## Export PNG

Tap **Export** on the HUD bar (or use the share/save sheet it opens), then choose:

- **Share…** — opens the system share sheet with a PNG of what you see on screen
  (no network permission; you pick the destination app).
- **Save to gallery** — on Android 10 and newer, saves to **Pictures/Fractals**.
  On older Android versions, opens the system save dialog so you can pick a folder.

The export captures the fractal view only (not the HUD or status overlay).

## Privacy

The app has no network access, no ads, no trackers, no analytics. Everything
renders locally on your device. See [PRIVACY.md](PRIVACY.md).
