package org.girino.frac.palettes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.girino.frac.operators.Complex;
import org.girino.frac.operators.FractalOperator;
import org.girino.frac.operators.OptimizedMandelbrotOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PaletteProviderTest {

    static Stream<PaletteProvider> uiPalettes() {
        return Stream.of(
                new DefaultPaletteGreen(),
                new DefaultPaletteBlue(),
                new DefaultPaletteRed(),
                new HSBPalette(),
                new SmoothFixedPalette(),
                new DefaultPalette());
    }

    @ParameterizedTest
    @MethodSource("uiPalettes")
    void interiorValueIsBlack(PaletteProvider palette) {
        assertEquals(Argb.BLACK, palette.getColor(1.0));
        assertEquals(Argb.BLACK, palette.getColor(1.0 - PaletteProvider.epsilon / 2.0));
    }

    @Test
    void fastEscapeAtHighMaxIterIsNotBlackOnNeonAndRgb() {
        double fastEscape = 1.0 / 4096.0;
        assertNotEquals(Argb.BLACK, new NeonPalette().getColor(fastEscape));
        assertNotEquals(Argb.BLACK, new DefaultPalette().getColor(fastEscape));
    }

    @Test
    void smoothInteriorValueMapsToBlack() {
        OptimizedMandelbrotOperator mandelbrot = new OptimizedMandelbrotOperator();
        FractalOperator.EscapeSample interior =
                mandelbrot.sample(new Complex(0, 0), 4096, true);
        assertTrue(!interior.escaped);
        assertEquals(Argb.BLACK, new NeonPalette().getColor(interior.value));
        assertEquals(Argb.BLACK, new DefaultPalette().getColor(interior.value));
    }

    @ParameterizedTest
    @MethodSource("uiPalettes")
    void midToneIsOpaqueNonBlack(PaletteProvider palette) {
        int color = palette.getColor(0.5);
        assertEquals(0xFF, (color >>> 24) & 0xff);
        assertNotEquals(Argb.BLACK, color);
    }

    @Test
    void greenPaletteEmphasizesGreenChannelAtMidTone() {
        int color = new DefaultPaletteGreen().getColor(0.5);
        assertTrue(Argb.green(color) >= Argb.red(color));
        assertTrue(Argb.green(color) >= Argb.blue(color) || Argb.blue(color) >= 0);
    }

    @Test
    void bluePaletteEmphasizesBlueishMixAtMidTone() {
        int color = new DefaultPaletteBlue().getColor(0.5);
        assertTrue(Argb.blue(color) >= 0);
        assertNotEquals(Argb.BLACK, color);
    }

    @Test
    void redPaletteEmphasizesReddishMixAtMidTone() {
        int color = new DefaultPaletteRed().getColor(0.5);
        assertTrue(Argb.red(color) >= 0);
        assertNotEquals(Argb.BLACK, color);
    }

    @Test
    void hsbPaletteChangesHueWithValue() {
        HSBPalette palette = new HSBPalette();
        assertNotEquals(palette.getColor(0.2), palette.getColor(0.6));
    }

    @Test
    void smoothFixedPaletteIndexesLutWithoutOverflow() {
        SmoothFixedPalette palette = new SmoothFixedPalette();
        int nearEnd = palette.getColor(1.0 - 2 * PaletteProvider.epsilon);
        assertNotEquals(Argb.BLACK, nearEnd);
        assertEquals(0xFF, (nearEnd >>> 24) & 0xff);
    }
}
