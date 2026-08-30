package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** SharedPreferences round-trip for fractal session (issue #19). */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class SessionStoreTest {

    private static final double EPS = 1e-9;

    @After
    public void tearDown() {
        SessionStore.clear(RuntimeEnvironment.getApplication());
    }

    @Test
    public void load_withoutSave_returnsNull() {
        assertNull(SessionStore.load(RuntimeEnvironment.getApplication()));
    }

    @Test
    public void saveLoad_roundTripPreservesSession() {
        ViewportSession original = new ViewportSession(
                -0.75,
                0.1,
                1.23456789e6,
                1080,
                2400,
                2,
                4,
                true);
        SessionStore.save(RuntimeEnvironment.getApplication(), original);
        ViewportSession loaded = SessionStore.load(RuntimeEnvironment.getApplication());

        assertNotNull(loaded);
        assertEquals(original.centerX, loaded.centerX, EPS);
        assertEquals(original.centerY, loaded.centerY, EPS);
        assertEquals(original.scale, loaded.scale, EPS);
        assertEquals(original.viewWidth, loaded.viewWidth);
        assertEquals(original.viewHeight, loaded.viewHeight);
        assertEquals(original.operatorIndex, loaded.operatorIndex);
        assertEquals(original.paletteIndex, loaded.paletteIndex);
        assertEquals(original.smooth, loaded.smooth);
    }

    @Test
    public void load_clampsCatalogIndices() {
        ViewportSession outOfRange = new ViewportSession(
                0, 0, 100, 64, 64, 999, -1, false);
        SessionStore.save(RuntimeEnvironment.getApplication(), outOfRange);
        ViewportSession loaded = SessionStore.load(RuntimeEnvironment.getApplication());

        assertNotNull(loaded);
        assertEquals(FormulaCatalog.size() - 1, loaded.operatorIndex);
        assertEquals(0, loaded.paletteIndex);
    }

    @Test
    public void clear_removesSavedSession() {
        SessionStore.save(
                RuntimeEnvironment.getApplication(),
                new ViewportSession(0, 0, 100, 64, 64, 0, 0, false));
        SessionStore.clear(RuntimeEnvironment.getApplication());
        assertNull(SessionStore.load(RuntimeEnvironment.getApplication()));
    }
}
