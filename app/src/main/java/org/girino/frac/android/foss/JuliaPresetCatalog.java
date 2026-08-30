package org.girino.frac.android.foss;

import java.util.Locale;

/** Preset Julia seeds c in z(n+1) = z(n)^2 + c. */
public final class JuliaPresetCatalog {

    public static final class Preset {
        public final int labelRes;
        public final double cRe;
        public final double cIm;

        Preset(int labelRes, double cRe, double cIm) {
            this.labelRes = labelRes;
            this.cRe = cRe;
            this.cIm = cIm;
        }
    }

    private static final Preset[] PRESETS = {
            new Preset(R.string.julia_preset_classic, 0.285, 0.013),
            new Preset(R.string.julia_preset_dendrite, -0.8, 0.156),
            new Preset(R.string.julia_preset_san_marco, -0.7269, 0.1889),
            new Preset(R.string.julia_preset_spiral, -0.4, 0.6),
            new Preset(R.string.julia_preset_douady, -0.123, 0.745),
            new Preset(R.string.julia_preset_phoenix_real, 0.566, 0.0),
    };

    private static final double MATCH_EPS = 1e-9;

    private JuliaPresetCatalog() {
    }

    public static int presetCount() {
        return PRESETS.length;
    }

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
            throw new IndexOutOfBoundsException("julia preset " + index);
        }
        return PRESETS[index];
    }

    public static int indexOfPreset(double cRe, double cIm) {
        for (int i = 0; i < PRESETS.length; i++) {
            Preset preset = PRESETS[i];
            if (matches(preset.cRe, preset.cIm, cRe, cIm)) {
                return i;
            }
        }
        return -1;
    }

    public static int pickerCheckedRow(double cRe, double cIm) {
        int preset = indexOfPreset(cRe, cIm);
        return preset >= 0 ? preset : customRowIndex();
    }

    public static boolean matches(double aRe, double aIm, double bRe, double bIm) {
        return Math.abs(aRe - bRe) <= MATCH_EPS && Math.abs(aIm - bIm) <= MATCH_EPS;
    }

    public static String formatShort(double cRe, double cIm) {
        return PhoenixPresetCatalog.formatShort(cRe, cIm);
    }

    public static String formatEditableComponent(double value) {
        return PhoenixPresetCatalog.formatEditableComponent(value);
    }
}
