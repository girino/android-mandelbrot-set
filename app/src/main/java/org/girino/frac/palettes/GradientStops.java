package org.girino.frac.palettes;

/** RGB multi-stop gradients for catalog palettes. */
final class GradientStops {

    private GradientStops() {
    }

    static int[] buildLut(int... stops) {
        return PaletteLut.build(value -> sample(stops, value));
    }

    static int sample(int[] stops, double value) {
        if (stops.length == 0) {
            return Argb.BLACK;
        }
        if (stops.length == 1) {
            return stops[0];
        }
        if (value <= 0.0) {
            return stops[0];
        }
        if (value >= 1.0) {
            return stops[stops.length - 1];
        }
        double scaled = value * (stops.length - 1);
        int index = (int) scaled;
        if (index >= stops.length - 1) {
            return stops[stops.length - 1];
        }
        float t = (float) (scaled - index);
        return lerp(stops[index], stops[index + 1], t);
    }

    private static int lerp(int from, int to, float t) {
        int r = (int) (Argb.red(from) + t * (Argb.red(to) - Argb.red(from)));
        int g = (int) (Argb.green(from) + t * (Argb.green(to) - Argb.green(from)));
        int b = (int) (Argb.blue(from) + t * (Argb.blue(to) - Argb.blue(from)));
        return Argb.rgb(r, g, b);
    }
}
