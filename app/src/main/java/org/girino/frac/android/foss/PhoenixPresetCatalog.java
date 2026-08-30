package org.girino.frac.android.foss;

import java.util.Locale;

/** Preset p values for Phoenix (z(n+1) = z(n)^2 + c + p*z(n-1)). */
public final class PhoenixPresetCatalog {

    public static final class Preset {
        public final int labelRes;
        public final double pRe;
        public final double pIm;

        Preset(int labelRes, double pRe, double pIm) {
            this.labelRes = labelRes;
            this.pRe = pRe;
            this.pIm = pIm;
        }
    }

    private static final Preset[] PRESETS = {
            new Preset(R.string.phoenix_preset_classic, -0.5, 0.0),
            new Preset(R.string.phoenix_preset_alternate, 0.5, 0.0),
            new Preset(R.string.phoenix_preset_mandelbrot, 0.0, 0.0),
            new Preset(R.string.phoenix_preset_complex_pp, 0.5, 0.5),
            new Preset(R.string.phoenix_preset_complex_nm, -0.5, 0.5),
            new Preset(R.string.phoenix_preset_imaginary, 0.0, 0.5),
            new Preset(R.string.phoenix_preset_light, -0.25, 0.0),
            new Preset(R.string.phoenix_preset_strong, -1.0, 0.0),
    };

    private static final double MATCH_EPS = 1e-9;

    private PhoenixPresetCatalog() {
    }

    public static int presetCount() {
        return PRESETS.length;
    }

    /** Preset rows plus the trailing Custom entry in the picker. */
    public static int pickerRowCount() {
        return PRESETS.length + 1;
    }

    public static int customRowIndex() {
        return PRESETS.length;
    }

    public static boolean isCustomRow(int rowIndex) {
        return rowIndex == customRowIndex();
    }

    public static Preset getPreset(int index) {
        if (index < 0 || index >= PRESETS.length) {
            throw new IndexOutOfBoundsException("phoenix preset " + index);
        }
        return PRESETS[index];
    }

    /** Index of the matching preset, or -1 when the current value is custom. */
    public static int indexOfPreset(double pRe, double pIm) {
        for (int i = 0; i < PRESETS.length; i++) {
            Preset preset = PRESETS[i];
            if (matches(preset.pRe, preset.pIm, pRe, pIm)) {
                return i;
            }
        }
        return -1;
    }

    /** Checked row in the preset picker: preset index or the Custom row. */
    public static int pickerCheckedRow(double pRe, double pIm) {
        int preset = indexOfPreset(pRe, pIm);
        return preset >= 0 ? preset : customRowIndex();
    }

    public static boolean matches(double aRe, double aIm, double bRe, double bIm) {
        return Math.abs(aRe - bRe) <= MATCH_EPS && Math.abs(aIm - bIm) <= MATCH_EPS;
    }

    /** Compact label for menu indicator and overlay (e.g. "-0.5", "0.5+0.5i"). */
    public static String formatShort(double pRe, double pIm) {
        if (Math.abs(pIm) <= MATCH_EPS) {
            return formatComponent(pRe);
        }
        if (Math.abs(pRe) <= MATCH_EPS) {
            return formatImagComponent(pIm);
        }
        return formatComponent(pRe) + formatImagComponent(pIm);
    }

    /** Plain decimal text for custom input fields. */
    public static String formatEditableComponent(double value) {
        if (Math.abs(value - Math.rint(value)) < MATCH_EPS) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.8g", value);
    }

    private static String formatComponent(double value) {
        if (Math.abs(value - Math.rint(value)) < MATCH_EPS) {
            return String.format(Locale.US, "%.0f", value);
        }
        if (Math.abs(2 * value - Math.rint(2 * value)) < MATCH_EPS) {
            return String.format(Locale.US, "%.1f", value);
        }
        return String.format(Locale.US, "%.4g", value);
    }

    private static String formatImagComponent(double pIm) {
        if (Math.abs(pIm - Math.rint(pIm)) < MATCH_EPS) {
            return String.format(Locale.US, "%+.0fi", pIm);
        }
        if (Math.abs(2 * pIm - Math.rint(2 * pIm)) < MATCH_EPS) {
            return String.format(Locale.US, "%+.1fi", pIm);
        }
        return String.format(Locale.US, "%+.4gi", pIm);
    }
}
