package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.girino.frac.operators.OptimizedMandelbrotOperator;
import org.girino.frac.palettes.HSBPalette;
import org.junit.Test;

/** Catalog indices for formula/palette pickers (issue #10). */
public class CatalogTest {

    @Test
    public void formulaCatalog_defaultsAndLookup() {
        assertEquals(7, FormulaCatalog.size());
        assertEquals(0, FormulaCatalog.indexOf(new OptimizedMandelbrotOperator()));
        assertEquals("Mandelbrot Set", FormulaCatalog.labels()[0]);
        assertTrue(FormulaCatalog.get(0) instanceof OptimizedMandelbrotOperator);
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
