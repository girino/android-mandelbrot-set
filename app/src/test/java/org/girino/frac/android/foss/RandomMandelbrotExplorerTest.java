package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.girino.frac.operators.JuliaOperator;
import org.girino.frac.operators.OptimizedMandelbrotOperator;

import java.util.Random;

/** Random boundary exploration is reproducible for a supplied seed. */
public class RandomMandelbrotExplorerTest {

    private static final double EPS = 1e-12;

    @Test
    public void seededExploration_isReproducibleAndZoomsIn() {
        RandomMandelbrotExplorer.Result first = RandomMandelbrotExplorer.explore(
                new OptimizedMandelbrotOperator(), new Random(42L), 16.0 / 9.0, () -> false);
        RandomMandelbrotExplorer.Result second = RandomMandelbrotExplorer.explore(
                new OptimizedMandelbrotOperator(), new Random(42L), 16.0 / 9.0, () -> false);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first.centerX, second.centerX, EPS);
        assertEquals(first.centerY, second.centerY, EPS);
        assertEquals(first.viewWidth, second.viewWidth, EPS);
        assertTrue(first.viewWidth <= 3.5 / Math.pow(RandomMandelbrotExplorer.MIN_ZOOM, 3));
    }

    @Test
    public void cancelledExploration_returnsNoViewport() {
        assertNull(RandomMandelbrotExplorer.explore(
                new OptimizedMandelbrotOperator(), new Random(42L), 1.0, () -> true));
    }

    @Test
    public void currentFormula_canDriveExploration() {
        RandomMandelbrotExplorer.Result result = RandomMandelbrotExplorer.explore(
                new JuliaOperator(0.285, 0.013), new Random(42L), 1.0, () -> false);

        assertNotNull(result);
        assertTrue(result.viewWidth < 3.5);
    }
}
