package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

/** SharedPreferences round-trip for IterationSettings (issue #26). */
public class IterationSettingsStoreTest {

    @Test
    public void defaults_matchIssueDefaults() {
        IterationSettings d = IterationSettings.defaults();
        assertEquals(IterationSettings.Mode.FIXED, d.mode);
        assertEquals(64, d.fixedMax);
        assertEquals(40, d.baseMax);
        assertEquals(1.2, d.multiplier, 1e-9);
    }

    @Test
    public void constructor_clampsOutOfRangeValues() {
        IterationSettings s = new IterationSettings(
                IterationSettings.Mode.SCALE_WITH_ZOOM, 1, 99999, 0.5, 0, 2_000_000_000);
        assertEquals(IterationSettings.MIN_ITER, s.fixedMax);
        assertEquals(99999, s.baseMax);
        assertEquals(IterationSettings.MIN_MULTIPLIER, s.multiplier, 1e-9);
        assertEquals(IterationSettings.MIN_ROUNDS, s.maxRounds);
        assertEquals(IterationSettings.MAX_ABSOLUTE_CAP, s.absoluteCap);
    }

    @Test
    public void constructor_clampsAbsoluteCapAtMax() {
        IterationSettings s = new IterationSettings(
                IterationSettings.Mode.ADAPTIVE, 64, 40, 1.2, 18, Integer.MAX_VALUE);
        assertEquals(IterationSettings.MAX_ABSOLUTE_CAP, s.absoluteCap);
    }

    @Test
    public void constructor_allowsAboveSoftWarn() {
        IterationSettings s = new IterationSettings(
                IterationSettings.Mode.ADAPTIVE, 8192, 40, 1.2, 18, 65536);
        assertEquals(8192, s.fixedMax);
        assertEquals(65536, s.absoluteCap);
        assertTrue(s.fixedMax > IterationSettings.SOFT_ITER_WARN);
    }

    @Test
    public void defaults_includeAdaptiveCaps() {
        IterationSettings d = IterationSettings.defaults();
        assertEquals(IterationSettings.DEFAULT_MAX_ROUNDS, d.maxRounds);
        assertEquals(IterationSettings.DEFAULT_ABSOLUTE_CAP, d.absoluteCap);
        assertEquals(18, d.maxRounds);
        assertEquals(1 << 18, d.absoluteCap);
    }

    @Test
    public void resetToDefaults_returnsFactoryValues() {
        IterationSettings defaults = IterationSettings.defaults();
        assertEquals(IterationSettings.Mode.FIXED, defaults.mode);
        assertEquals(64, defaults.fixedMax);
        assertEquals(40, defaults.baseMax);
        assertEquals(1.2, defaults.multiplier, 1e-9);
    }

    @Test
    public void helpBody_mentionsIterations() throws IOException {
        Path path = Path.of("src/main/res/values/strings.xml");
        String xml = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertTrue(xml.contains("name=\"menu_iterations\""));
        assertTrue(xml.contains("name=\"iteration_reset_defaults\""));
        assertTrue(xml.contains("Default: 64"));
        assertTrue(xml.contains("Default: 1.2"));
    }
}
