package org.girino.frac.palettes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PaletteLutTest {

    @Test
    void lutMatchesDirectFormulaAtLutSamples() {
        for (int i = 1; i < PaletteLut.SIZE - 1; i++) {
            double value = i / (double) PaletteLut.SIZE;
            assertEquals(DefaultPaletteGreen.colorAt(value), new DefaultPaletteGreen().getColor(value));
            assertEquals(DefaultPaletteBlue.colorAt(value), new DefaultPaletteBlue().getColor(value));
            assertEquals(DefaultPaletteRed.colorAt(value), new DefaultPaletteRed().getColor(value));
            assertEquals(DefaultPalette.colorAt(value), new DefaultPalette().getColor(value));
            assertEquals(HSBPalette.colorAt(value), new HSBPalette().getColor(value));
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.001, 0.123, 0.456, 0.888})
    void lookupIndexMatchesSmoothFixedStyle(double value) {
        int[] lut = PaletteLut.build(v -> DefaultPaletteGreen.colorAt(v));
        assertEquals(lut[(int) (lut.length * value)], PaletteLut.lookup(lut, value));
    }
}
