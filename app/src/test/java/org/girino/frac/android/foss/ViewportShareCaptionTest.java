package org.girino.frac.android.foss;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Share caption text for viewport PNG export. */
public class ViewportShareCaptionTest {

    @Test
    public void build_includesAttributionLinksAndViewport() {
        String text = ViewportShareCaption.build(
                "1.2.0",
                "Tricorn",
                -0.745,
                0.112,
                2500.0,
                FormulaCatalog.TRICORN_INDEX,
                0, 0, 0, 0);
        assertTrue(text.contains("Generated with Fractals by Girino FOSS v1.2.0"));
        assertTrue(text.contains(ViewportShareCaption.GITHUB_URL));
        assertTrue(text.contains(ViewportShareCaption.ZAPSTORE_URL));
        assertTrue(text.contains("Formula: Tricorn"));
        assertTrue(text.contains("Center: -0.745"));
        assertTrue(text.contains("Scale: 2500"));
        assertFalse(text.contains("Julia c:"));
        assertFalse(text.contains("Phoenix p:"));
    }

    @Test
    public void build_juliaIncludesC() {
        String text = ViewportShareCaption.build(
                "1.2.0",
                "Julia",
                0, 0, 100,
                FormulaCatalog.JULIA_INDEX,
                -0.4, 0.6, 0, 0);
        assertTrue(text.contains("Julia c:"));
        assertTrue(text.contains("-0.4"));
        assertTrue(text.contains("0.6"));
        assertFalse(text.contains("Phoenix p:"));
    }

    @Test
    public void build_phoenixIncludesP() {
        String text = ViewportShareCaption.build(
                "1.2.0",
                "Phoenix",
                0, 0, 100,
                FormulaCatalog.PHOENIX_INDEX,
                0, 0, -0.5, 0.25);
        assertFalse(text.contains("Julia c:"));
        assertTrue(text.contains("Phoenix p:"));
        assertTrue(text.contains("-0.5"));
    }

    @Test
    public void build_juliaPhoenixIncludesBothParams() {
        String text = ViewportShareCaption.build(
                "1.2.0",
                "Julia Phoenix",
                0, 0, 100,
                FormulaCatalog.JULIA_PHOENIX_INDEX,
                -0.4, 0.6, -0.5, 0.0);
        assertTrue(text.contains("Julia c:"));
        assertTrue(text.contains("Phoenix p:"));
    }
}
