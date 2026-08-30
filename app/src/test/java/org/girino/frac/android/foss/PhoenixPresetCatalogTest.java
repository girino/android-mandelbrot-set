package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PhoenixPresetCatalogTest {

    @Test
    public void indexOfPreset_findsClassicDefault() {
        assertEquals(0, PhoenixPresetCatalog.indexOfPreset(-0.5, 0.0));
    }

    @Test
    public void indexOfPreset_unknownReturnsMinusOne() {
        assertEquals(-1, PhoenixPresetCatalog.indexOfPreset(0.123, 0.456));
    }

    @Test
    public void pickerCheckedRow_customWhenNoPresetMatches() {
        assertEquals(
                PhoenixPresetCatalog.customRowIndex(),
                PhoenixPresetCatalog.pickerCheckedRow(0.123, 0.456));
    }

    @Test
    public void formatShort_realAndComplexValues() {
        assertEquals("-0.5", PhoenixPresetCatalog.formatShort(-0.5, 0.0));
        assertEquals("0", PhoenixPresetCatalog.formatShort(0.0, 0.0));
        assertEquals("0.5+0.5i", PhoenixPresetCatalog.formatShort(0.5, 0.5));
        assertEquals("+0.5i", PhoenixPresetCatalog.formatShort(0.0, 0.5));
    }

    @Test
    public void matches_withinEpsilon() {
        assertTrue(PhoenixPresetCatalog.matches(-0.5, 0.0, -0.5 + 1e-12, 0.0));
    }
}
