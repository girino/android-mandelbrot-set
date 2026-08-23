package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.girino.frac.operators.FractalOperator;
import org.girino.frac.operators.OptimizedMandelbrotOperator;
import org.girino.frac.palettes.HSBPalette;
import org.girino.frac.palettes.PaletteProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Border-doubling adaptive refinement (issue #28). */
public class AdaptiveRefinerTest {

    private ExecutorService workers;

    /** Same home extent as MandelbrotView.reset for the given width. */
    private static double homeScale(int width) {
        return 100.0 * 300.0 / 320.0 * width / 320.0;
    }

    @Before
    public void setUp() {
        workers = Executors.newFixedThreadPool(2);
    }

    @After
    public void tearDown() {
        workers.shutdownNow();
    }

    @Test
    public void collectBorder_findsInteriorNextToEscaped() {
        // 3x3: center interior, one escaped neighbor → center is border.
        boolean[] interior = {
                false, false, false,
                false, true, false,
                false, false, false
        };
        int[] border = new int[9];
        int count = AdaptiveRefiner.collectBorder(interior, 3, 3, border);
        assertEquals(1, count);
        assertEquals(4, border[0]);
    }

    @Test
    public void collectBorder_skipsInteriorFarFromEdge() {
        boolean[] interior = {
                true, true, true,
                true, true, true,
                true, true, true
        };
        int[] border = new int[9];
        assertEquals(0, AdaptiveRefiner.collectBorder(interior, 3, 3, border));
    }

    @Test
    public void refine_escapeClassification_matchesBruteForceAtFinalCap() {
        int width = 24;
        int height = 18;
        double scale = homeScale(width);
        int pass1 = 20;
        int cap = 80;
        int rounds = 8;
        PaletteProvider palette = new HSBPalette();
        FractalOperator[] ops = {
                new OptimizedMandelbrotOperator(),
                new OptimizedMandelbrotOperator()
        };

        int[] adaptive = new int[width * height];
        boolean[] interior = new boolean[width * height];
        Arrays.fill(adaptive, 0xff0a0a0a);
        AtomicInteger done = new AtomicInteger();
        assertTrue(ParallelStepRenderer.fillStep(
                adaptive, width, height, 1, scale, 0, 0,
                ops, palette, false, pass1,
                workers, null, done, width * height, null, interior));
        assertTrue(AdaptiveRefiner.refine(
                adaptive, interior, width, height, scale, 0, 0,
                ops, palette, false, pass1, rounds, cap,
                workers, null, done, width * height * 2, null));

        OptimizedMandelbrotOperator probe = new OptimizedMandelbrotOperator();
        org.girino.frac.operators.Complex point = new org.girino.frac.operators.Complex();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                point.set(
                        (x - width / 2.0) / scale,
                        (y - height / 2.0) / scale);
                boolean escapedAtCap = probe.sample(point, cap, false).escaped;
                assertEquals(
                        "pixel " + x + "," + y,
                        !escapedAtCap,
                        interior[y * width + x]);
            }
        }
    }

    @Test
    public void refine_stabilizesBorderAtEachLimit() {
        int width = 32;
        int height = 24;
        double scale = homeScale(width);
        int pass1 = 10;
        int cap = 160;
        PaletteProvider palette = new HSBPalette();
        FractalOperator[] ops = {
                new OptimizedMandelbrotOperator(),
                new OptimizedMandelbrotOperator()
        };
        int[] pixels = new int[width * height];
        boolean[] interior = new boolean[width * height];
        Arrays.fill(pixels, 0xff0a0a0a);
        AtomicInteger done = new AtomicInteger();
        AtomicInteger publishes = new AtomicInteger();
        assertTrue(ParallelStepRenderer.fillStep(
                pixels, width, height, 1, scale, 0, 0,
                ops, palette, false, pass1,
                workers, null, done, width * height, null, interior));
        int borderBefore = AdaptiveRefiner.collectBorder(
                interior, width, height, new int[width * height]);
        assertTrue("expected an interior/exterior border at low pass1", borderBefore > 0);

        assertTrue(AdaptiveRefiner.refine(
                pixels, interior, width, height, scale, 0, 0,
                ops, palette, false, pass1, 8, cap,
                workers, null, done, width * height * 4, null,
                (px, w, h, limit) -> publishes.incrementAndGet()));

        assertTrue("expected at least one border-fill publish", publishes.get() > 0);
        // After stabilize+doubling up to cap, classification matches brute at cap.
        OptimizedMandelbrotOperator probe = new OptimizedMandelbrotOperator();
        org.girino.frac.operators.Complex point = new org.girino.frac.operators.Complex();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                point.set(
                        (x - width / 2.0) / scale,
                        (y - height / 2.0) / scale);
                boolean escapedAtCap = probe.sample(point, cap, false).escaped;
                assertEquals(
                        "pixel " + x + "," + y,
                        !escapedAtCap,
                        interior[y * width + x]);
            }
        }
    }

    @Test
    public void refine_stopsWhenCancelled() {
        int width = 64;
        int height = 48;
        double scale = homeScale(width);
        PaletteProvider palette = new HSBPalette();
        FractalOperator[] ops = {
                new OptimizedMandelbrotOperator(),
                new OptimizedMandelbrotOperator()
        };
        int[] pixels = new int[width * height];
        boolean[] interior = new boolean[width * height];
        Arrays.fill(pixels, 0xff0a0a0a);
        AtomicInteger done = new AtomicInteger();
        assertTrue(ParallelStepRenderer.fillStep(
                pixels, width, height, 1, scale, 0, 0,
                ops, palette, false, 16,
                workers, null, done, width * height, null, interior));

        boolean finished = AdaptiveRefiner.refine(
                pixels, interior, width, height, scale, 0, 0,
                ops, palette, false, 16, 8, 4096,
                workers,
                () -> true,
                done, width * height * 4, null);
        assertFalse(finished);
    }

    @Test
    public void adaptiveMode_resolveMaxIter_usesFixedMax() {
        IterationSettings settings = new IterationSettings(
                IterationSettings.Mode.ADAPTIVE, 55, 40, 1.2, 4, 512);
        double home = IterationPolicy.referenceScale(320);
        assertEquals(55, IterationPolicy.resolveMaxIter(settings, home, 320));
        assertEquals(55, IterationPolicy.resolveMaxIter(settings, home * 8, 320));
    }
}
