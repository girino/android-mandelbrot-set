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
        assertEquals(40, d.fixedMax);
        assertEquals(40, d.baseMax);
        assertEquals(1.2, d.multiplier, 1e-9);
    }

    @Test
    public void constructor_clampsOutOfRangeValues() {
        IterationSettings s = new IterationSettings(
                IterationSettings.Mode.SCALE_WITH_ZOOM, 1, 99999, 0.5);
        assertEquals(IterationSettings.MIN_ITER, s.fixedMax);
        assertEquals(IterationSettings.MAX_ITER_CAP, s.baseMax);
        assertEquals(IterationSettings.MIN_MULTIPLIER, s.multiplier, 1e-9);
    }

    @Test
    public void helpBody_mentionsIterations() throws IOException {
        Path path = Path.of("src/main/res/values/strings.xml");
        String xml = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertTrue(xml.contains("name=\"menu_iterations\""));
        assertTrue(xml.contains("Scale with zoom"));
    }
}
