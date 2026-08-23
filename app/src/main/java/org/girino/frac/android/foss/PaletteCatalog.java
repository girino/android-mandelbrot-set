package org.girino.frac.android.foss;

import org.girino.frac.palettes.DefaultPaletteBlue;
import org.girino.frac.palettes.DefaultPaletteGreen;
import org.girino.frac.palettes.DefaultPaletteRed;
import org.girino.frac.palettes.HSBPalette;
import org.girino.frac.palettes.PaletteProvider;
import org.girino.frac.palettes.SmoothFixedPalette;

/** Named palettes available in the picker (issues #10 / #13). */
public final class PaletteCatalog {
    private static final String[] LABELS = {
            "Green",
            "Blue",
            "Red",
            "Rainbow",
            "Spectrum",
    };

    private static final PaletteProvider[] PALETTES = {
            new DefaultPaletteGreen(),
            new DefaultPaletteBlue(),
            new DefaultPaletteRed(),
            new HSBPalette(),
            new SmoothFixedPalette(),
    };

    private PaletteCatalog() {
    }

    public static String[] labels() {
        return LABELS.clone();
    }

    public static int size() {
        return PALETTES.length;
    }

    public static PaletteProvider get(int index) {
        return PALETTES[index];
    }

    /** Index of the first catalog entry with the same class, or -1. */
    public static int indexOf(PaletteProvider palette) {
        if (palette == null) {
            return -1;
        }
        Class<?> type = palette.getClass();
        for (int i = 0; i < PALETTES.length; i++) {
            if (PALETTES[i].getClass() == type) {
                return i;
            }
        }
        return -1;
    }
}
