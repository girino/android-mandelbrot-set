package org.girino.frac.palettes;

/**
 * Pure-Java ARGB helpers matching android.graphics.Color packing for the subset
 * this app needs, so palettes stay unit-testable on the JVM.
 */
public final class Argb {
    public static final int BLACK = 0xFF000000;

    private Argb() {
    }

    public static int rgb(int red, int green, int blue) {
        return 0xFF000000
                | ((red & 0xff) << 16)
                | ((green & 0xff) << 8)
                | (blue & 0xff);
    }

    /**
     * Converts HSV to opaque ARGB.
     * hsv[0] is hue in degrees 0..360, hsv[1] saturation and hsv[2] value in 0..1,
     * matching android.graphics.Color.HSVToColor.
     */
    public static int hsvToColor(float[] hsv) {
        float h = hsv[0];
        float s = hsv[1];
        float v = hsv[2];

        if (s <= 0f) {
            int grey = clampByte((int) (v * 255f + 0.5f));
            return rgb(grey, grey, grey);
        }

        h = ((h % 360f) + 360f) % 360f;
        float sector = h / 60f;
        int i = (int) sector;
        float f = sector - i;
        float p = v * (1f - s);
        float q = v * (1f - s * f);
        float t = v * (1f - s * (1f - f));

        float r;
        float g;
        float b;
        switch (i) {
            case 0:
                r = v;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v;
                b = p;
                break;
            case 2:
                r = p;
                g = v;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v;
                break;
            case 4:
                r = t;
                g = p;
                b = v;
                break;
            default:
                r = v;
                g = p;
                b = q;
                break;
        }

        return rgb(
                clampByte((int) (r * 255f + 0.5f)),
                clampByte((int) (g * 255f + 0.5f)),
                clampByte((int) (b * 255f + 0.5f)));
    }

    public static int red(int color) {
        return (color >> 16) & 0xff;
    }

    public static int green(int color) {
        return (color >> 8) & 0xff;
    }

    public static int blue(int color) {
        return color & 0xff;
    }

    private static int clampByte(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 255) {
            return 255;
        }
        return value;
    }
}
