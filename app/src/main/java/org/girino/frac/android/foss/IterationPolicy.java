package org.girino.frac.android.foss;

/**
 * Resolves escape-time maxIter from IterationSettings and viewport scale
 * (issues #26 / #27).
 *
 * SCALE_WITH_ZOOM: each doubling of scale relative to the home reference
 * multiplies maxIter by the user multiplier:
 * maxIter = round(base * multiplier ^ log2(scale / referenceScale)),
 * then clamped to [MIN_ITER, MAX_ITER_CAP].
 */
public final class IterationPolicy {

    private IterationPolicy() {
    }

    /** Home zoom scale for a given view width (matches MandelbrotView reset). */
    public static double referenceScale(int viewWidth) {
        if (viewWidth <= 0) {
            viewWidth = 320;
        }
        return 100.0 * 300.0 / viewWidth;
    }

    public static int resolveMaxIter(IterationSettings settings, double scale, int viewWidth) {
        if (settings == null || settings.mode == IterationSettings.Mode.FIXED) {
            int fixed = settings != null ? settings.fixedMax : IterationSettings.DEFAULT_FIXED_MAX;
            return clamp(fixed);
        }
        double reference = referenceScale(viewWidth);
        if (!(scale > 0) || !(reference > 0)) {
            return clamp(settings.baseMax);
        }
        double ratio = scale / reference;
        double zoomSteps = Math.log(ratio) / Math.log(2.0);
        double raw = settings.baseMax * Math.pow(settings.multiplier, zoomSteps);
        if (!Double.isFinite(raw) || raw >= IterationSettings.MAX_ITER_CAP) {
            return IterationSettings.MAX_ITER_CAP;
        }
        if (raw <= IterationSettings.MIN_ITER) {
            return IterationSettings.MIN_ITER;
        }
        return (int) Math.round(raw);
    }

    private static int clamp(int value) {
        return Math.max(
                IterationSettings.MIN_ITER,
                Math.min(IterationSettings.MAX_ITER_CAP, value));
    }
}
