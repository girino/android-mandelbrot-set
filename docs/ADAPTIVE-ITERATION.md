# Adaptive iteration (issue #28)

## Chosen algorithm: border doubling

After the progressive fill reaches step 1 at the configured pass-1 max
iterations (`IterationSettings.fixedMax` in Adaptive mode):

1. Mark each pixel as **interior** if the orbit did not escape before that
   limit (`FractalOperator.EscapeSample.escaped == false`).
2. Double the limit (`next = min(2 × current, absoluteCap)`).
3. Collect the **internal border**: interior pixels with at least one
   4-connected escaped neighbor, **always unioned with the image perimeter**
   (screen edges are always frontier). If there is no seam and the frame is
   still all interior, the perimeter alone seeds the refine.
4. Re-test only those border pixels at `next`.
5. If **some** escape, update colors / interior flags, publish an intermediate
   frame, and go back to step 3 **at the same limit** (stabilize until no new
   border is filled).
6. If **none** escape at this limit, advance `current` to `next`. Stop only
   when that empty pass happens **and** `current >= adaptiveMaxIter` from the
   value shown on the status overlay at the start of this refine (same field
   written by border-round publishes). While still under that floor, keep
   doubling even past `maxRounds`, and refresh the overlay Iter so outer
   borders keep intermediate colors.
7. If escapes were found, repeat from step 2 until `maxRounds` or the absolute
   cap is reached (empty passes below the previous-zoom floor also continue).
8. The status overlay **Iter** line shows the highest limit from the last
   finished border round (display only). Pass-1 after zoom stays at the
   configured Fixed max — it is not raised from the previous frame.
9. Colors are written only for retested border pixels (`sample.value` at the
   limit used for that retest). Earlier escaped pixels keep the color from
   the pass that first found them — no full-frame palette remap.

Refinement runs only after step 1 so coarse progressive blocks are not
re-tested. Border retests use **warm-start**: pass-1 stores each interior
pixel's orbit checkpoint (iteration count and Z); each border round
continues from that checkpoint via FractalOperator.sampleContinue instead
of restarting from iteration 0. Cancellation still honors `renderGeneration` and thread interrupt
(`ParallelStepRenderer.CancelCheck`). Workers reuse the same pool as issue #25.

## Alternatives not implemented (v1)

| Approach | Why deferred |
|----------|--------------|
| **Mariani–Silver** | Large wins on flat rectangles, but needs a rectangle stack and can miss cusps narrower than one pixel when the dwell limit is low. Formula-agnostic border doubling matched the multi-operator catalog with less risk. |
| **Border tracing** | Strong for specialized CPU renderers; more complex to keep correct with progressive publish and cancel. |
| **Interior fast-path** (cardioid / period-2 bulb) | Cheap at default Mandelbrot zoom only; not valid for Burning Ship, Nova, Mandelbar, etc. |

References for later spikes:

- [Mariani/Silver algorithm (Mu-Ency)](https://www.mrob.com/pub/muency/marianisilveralgorithm.html)
- [Wikipedia — Plotting algorithms for the Mandelbrot set](https://en.wikipedia.org/wiki/Plotting_algorithms_for_the_Mandelbrot_set)
- [Rico Mariani — Mariani-Silver explainer](https://ricomariani.medium.com/the-mariani-silver-algorithm-for-drawing-the-mandelbrot-set-a71e31bc20b6)
