package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** IterationPolicy scale mapping (issues #26 / #27). */
public class IterationPolicyTest {

    private static final int WIDTH = 320;
    private static final double EPS = 1e-9;

    @Test
    public void fixedMode_ignoresScale() {
        IterationSettings settings = new IterationSettings(
                IterationSettings.Mode.FIXED, 100, 40, 1.2);
        double home = IterationPolicy.referenceScale(WIDTH);
        assertEquals(100, IterationPolicy.resolveMaxIter(settings, home, WIDTH));
        assertEquals(100, IterationPolicy.resolveMaxIter(settings, home * 16, WIDTH));
    }

    @Test
    public void scaleMode_atHomeZoom_equalsBase() {
        IterationSettings settings = new IterationSettings(
                IterationSettings.Mode.SCALE_WITH_ZOOM, 40, 40, 1.2);
        double home = IterationPolicy.referenceScale(WIDTH);
        assertEquals(40, IterationPolicy.resolveMaxIter(settings, home, WIDTH));
    }

    @Test
    public void scaleMode_doubleScale_multipliesOnce() {
        IterationSettings settings = new IterationSettings(
                IterationSettings.Mode.SCALE_WITH_ZOOM, 40, 40, 1.2);
        double home = IterationPolicy.referenceScale(WIDTH);
        int expected = (int) Math.round(40 * 1.2);
        assertEquals(expected, IterationPolicy.resolveMaxIter(settings, home * 2, WIDTH));
    }

    @Test
    public void scaleMode_halfScale_dividesOnce() {
        IterationSettings settings = new IterationSettings(
                IterationSettings.Mode.SCALE_WITH_ZOOM, 40, 40, 1.2);
        double home = IterationPolicy.referenceScale(WIDTH);
        int expected = (int) Math.round(40 / 1.2);
        assertEquals(expected, IterationPolicy.resolveMaxIter(settings, home / 2, WIDTH));
    }

    @Test
    public void resolve_clampsToCap() {
        IterationSettings settings = new IterationSettings(
                IterationSettings.Mode.SCALE_WITH_ZOOM, 40, 1000, 4.0);
        double home = IterationPolicy.referenceScale(WIDTH);
        int result = IterationPolicy.resolveMaxIter(settings, home * Math.pow(2, 20), WIDTH);
        assertEquals(IterationSettings.MAX_ITER_CAP, result);
    }

    @Test
    public void referenceScale_matchesViewHomeFormula() {
        assertEquals(100.0 * 300.0 / 320.0, IterationPolicy.referenceScale(320), EPS);
        assertTrue(IterationPolicy.referenceScale(0) > 0);
    }
}
