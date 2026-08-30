package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.girino.frac.operators.OptimizedMandelbrotOperator;
import org.girino.frac.palettes.HSBPalette;
import org.junit.Test;

/** Catalog indices for formula/palette pickers (issue #10). */
public class CatalogTest {

    @Test
    public void formulaCatalog_defaultsAndLookup() {
        assertEquals(12, FormulaCatalog.size());
        assertEquals(0, FormulaCatalog.indexOf(new OptimizedMandelbrotOperator()));
        assertEquals("Mandelbrot Set", FormulaCatalog.labels()[0]);
        assertEquals("Tricorn", FormulaCatalog.labels()[FormulaCatalog.TRICORN_INDEX]);
        assertEquals("Phoenix", FormulaCatalog.labels()[FormulaCatalog.PHOENIX_INDEX]);
        assertEquals("Julia", FormulaCatalog.labels()[FormulaCatalog.JULIA_INDEX]);
        assertEquals("Julia Phoenix", FormulaCatalog.labels()[FormulaCatalog.JULIA_PHOENIX_INDEX]);
        assertEquals("Celtic Mandelbrot", FormulaCatalog.labels()[FormulaCatalog.CELTIC_INDEX]);
        assertEquals(
                "Perpendicular Mandelbrot",
                FormulaCatalog.labels()[FormulaCatalog.PERPENDICULAR_INDEX]);
        assertTrue(FormulaCatalog.get(0) instanceof OptimizedMandelbrotOperator);
        assertNotSame(FormulaCatalog.create(0), FormulaCatalog.create(0));
    }

    @Test
    public void paletteCatalog_defaultsAndLookup() {
        assertEquals(5, PaletteCatalog.size());
        assertEquals(3, PaletteCatalog.indexOf(new HSBPalette()));
        assertEquals("RGB", PaletteCatalog.labels()[3]);
        assertEquals("BGR", PaletteCatalog.labels()[4]);
        assertTrue(PaletteCatalog.get(3) instanceof HSBPalette);
    }
}
