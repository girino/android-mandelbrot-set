package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
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
    public void needsFrameSeed_trueWhenAllInterior() {
        boolean[] interior = {
                true, true, true,
                true, true, true,
                true, true, true
        };
        assertTrue(AdaptiveRefiner.needsFrameSeed(interior, 3, 3));
    }

    @Test
    public void needsFrameSeed_falseWhenSeamExists() {
        boolean[] interior = {
                false, false, false,
                false, true, false,
                false, false, false
        };
        assertTrue(!AdaptiveRefiner.needsFrameSeed(interior, 3, 3));
    }

    @Test
    public void refine_seedFloor_ignoresMaxRoundsWhileUnderFloor() {
        // maxRounds=1 would normally stop after the first empty double (2*pass1),
        // but the previous-zoom floor forces climbing all the way to seed.
        int width = 4;
        int height = 4;
        double deepScale = 1e12;
        PaletteProvider palette = new HSBPalette();
        FractalOperator[] ops = {new OptimizedMandelbrotOperator()};
        int pass1 = 10;
        int seed = 80;
        int cap = 160;
        boolean[] interior = new boolean[width * height];
        Arrays.fill(interior, true);
        int[] pixels = new int[width * height];
        Arrays.fill(pixels, 0xff000000);
        AtomicInteger done = new AtomicInteger();
        AtomicInteger publishes = new AtomicInteger();
        int reached = AdaptiveRefiner.refine(
                pixels, interior, width, height, deepScale, 0, 0,
                ops, palette, false, pass1, 1, cap,
                workers, null, done, width * height, null,
                (px, w, h, limit) -> publishes.incrementAndGet(),
                seed);
        assertEquals(seed, reached);
        assertTrue(publishes.get() >= 1);
    }

    @Test
    public void refine_seedFloor_climbsFromPass1UntilPreviousMax() {
        // No escapes: without seed, stop after first empty double (return 2*pass1).
        // With seed floor, keep doubling through intermediates until >= seed.
        int width = 4;
        int height = 4;
        double deepScale = 1e12;
        PaletteProvider palette = new HSBPalette();
        FractalOperator[] ops = {new OptimizedMandelbrotOperator()};
        int pass1 = 10;
        int seed = 40;
        int cap = 80;

        boolean[] interiorA = new boolean[width * height];
        Arrays.fill(interiorA, true);
        int[] pixelsA = new int[width * height];
        Arrays.fill(pixelsA, 0xff000000);
        AtomicInteger doneA = new AtomicInteger();
        int withoutSeed = AdaptiveRefiner.refine(
                pixelsA, interiorA, width, height, deepScale, 0, 0,
                ops, palette, false, pass1, 1, cap,
                workers, null, doneA, width * height, null, null, 0);
        assertEquals(pass1 * 2, withoutSeed);

        boolean[] interiorB = new boolean[width * height];
        Arrays.fill(interiorB, true);
        int[] pixelsB = new int[width * height];
        Arrays.fill(pixelsB, 0xff000000);
        AtomicInteger doneB = new AtomicInteger();
        int withSeed = AdaptiveRefiner.refine(
                pixelsB, interiorB, width, height, deepScale, 0, 0,
                ops, palette, false, pass1, 1, cap,
                workers, null, doneB, width * height, null, null, seed);
        assertEquals(seed, withSeed);
    }

    @Test
    public void collectBorder_alsoFrame_unionsFrameWithSeam() {
        // Escaped center creates a seam at the four neighbors; alsoFrame
        // adds remaining perimeter interior pixels (first-round / zoom-out).
        boolean[] interior = {
                true, true, true,
                true, false, true,
                true, true, true
        };
        int[] border = new int[9];
        int seamOnly = AdaptiveRefiner.collectBorder(interior, 3, 3, border);
        assertEquals(4, seamOnly);

        int[] border2 = new int[9];
        int withFrame = AdaptiveRefiner.collectBorder(
                interior, 3, 3, border2, true, new int[9], new boolean[9]);
        assertTrue(withFrame > seamOnly);
        assertEquals(8, withFrame);
    }

    @Test
    public void refine_alwaysIncludesScreenEdge_withSeam() {
        // Seam exists (one escaped corner): refine still runs and probes edges.
        int width = 4;
        int height = 4;
        double deepScale = 1e12;
        PaletteProvider palette = new HSBPalette();
        FractalOperator[] ops = {new OptimizedMandelbrotOperator()};
        boolean[] interior = new boolean[width * height];
        Arrays.fill(interior, true);
        interior[0] = false;
        int[] pixels = new int[width * height];
        Arrays.fill(pixels, 0xff000000);
        AtomicInteger done = new AtomicInteger();
        int reached = AdaptiveRefiner.refine(
                pixels, interior, width, height, deepScale, 0, 0,
                ops, palette, false, 10, 1, 40,
                workers, null, done, width * height, null, null, 0);
        assertEquals(20, reached);
    }

    @Test
    public void refine_seedFloorEvenWithSeam() {
        int width = 4;
        int height = 4;
        double deepScale = 1e12;
        PaletteProvider palette = new HSBPalette();
        FractalOperator[] ops = {new OptimizedMandelbrotOperator()};
        int pass1 = 10;
        int seed = 40;
        int cap = 80;

        // One escaped pixel → not all-interior; seed floor still forces climb.
        boolean[] interior = new boolean[width * height];
        Arrays.fill(interior, true);
        interior[0] = false;
        int[] pixels = new int[width * height];
        Arrays.fill(pixels, 0xff000000);
        AtomicInteger done = new AtomicInteger();
        int reached = AdaptiveRefiner.refine(
                pixels, interior, width, height, deepScale, 0, 0,
                ops, palette, false, pass1, 1, cap,
                workers, null, done, width * height, null, null, seed);
        assertEquals(seed, reached);
    }

    @Test
    public void collectBorder_allInterior_usesImageFrame() {
        boolean[] interior = {
                true, true, true,
                true, true, true,
                true, true, true
        };
        int[] border = new int[9];
        int count = AdaptiveRefiner.collectBorder(interior, 3, 3, border);
        // Perimeter only (8), center stays off the seed border.
        assertEquals(8, count);
        boolean[] seen = new boolean[9];
        for (int i = 0; i < count; i++) {
            seen[border[i]] = true;
        }
        assertTrue(seen[0] && seen[1] && seen[2]);
        assertTrue(seen[3] && seen[5]);
        assertTrue(seen[6] && seen[7] && seen[8]);
        assertTrue(!seen[4]);
    }

    @Test
    public void collectBorder_allEscaped_returnsEmpty() {
        boolean[] interior = {
                false, false, false,
                false, false, false,
                false, false, false
        };
        int[] border = new int[9];
        assertEquals(0, AdaptiveRefiner.collectBorder(interior, 3, 3, border));
    }

    @Test
    public void collectFrameInterior_skipsAlreadyEscapedEdges() {
        boolean[] interior = {
                false, true, false,
                true, true, true,
                false, true, false
        };
        int[] border = new int[9];
        int count = AdaptiveRefiner.collectFrameInterior(interior, 3, 3, border);
        // Top mid, bottom mid, left mid, right mid (corners already escaped).
        assertEquals(4, count);
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
        OrbitState orbit = new OrbitState(width * height);
        Arrays.fill(adaptive, 0xff0a0a0a);
        AtomicInteger done = new AtomicInteger();
        assertTrue(ParallelStepRenderer.fillStep(
                adaptive, width, height, 1, scale, 0, 0,
                ops, palette, false, pass1,
                workers, null, done, width * height, null, interior, orbit));
        assertTrue(AdaptiveRefiner.refine(
                adaptive, interior, width, height, scale, 0, 0,
                ops, palette, false, pass1, rounds, cap,
                workers, null, done, width * height * 2, null, null, 0, orbit) >= pass1);

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
        OrbitState orbit = new OrbitState(width * height);
        Arrays.fill(pixels, 0xff0a0a0a);
        AtomicInteger done = new AtomicInteger();
        AtomicInteger publishes = new AtomicInteger();
        assertTrue(ParallelStepRenderer.fillStep(
                pixels, width, height, 1, scale, 0, 0,
                ops, palette, false, pass1,
                workers, null, done, width * height, null, interior, orbit));
        int borderBefore = AdaptiveRefiner.collectBorder(
                interior, width, height, new int[width * height]);
        assertTrue("expected an interior/exterior border at low pass1", borderBefore > 0);

        int maxReached = AdaptiveRefiner.refine(
                pixels, interior, width, height, scale, 0, 0,
                ops, palette, false, pass1, 8, cap,
                workers, null, done, width * height * 4, null,
                (px, w, h, limit) -> publishes.incrementAndGet(), 0, orbit);
        assertTrue(maxReached >= pass1);
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
        OrbitState orbit = new OrbitState(width * height);
        Arrays.fill(pixels, 0xff0a0a0a);
        AtomicInteger done = new AtomicInteger();
        assertTrue(ParallelStepRenderer.fillStep(
                pixels, width, height, 1, scale, 0, 0,
                ops, palette, false, 16,
                workers, null, done, width * height, null, interior, orbit));

        int finished = AdaptiveRefiner.refine(
                pixels, interior, width, height, scale, 0, 0,
                ops, palette, false, 16, 8, 4096,
                workers,
                () -> true,
                done, width * height * 4, null, null, 0, orbit);
        assertEquals(-1, finished);
    }

    @Test
    public void refine_returnsMaxLimitReachedForCarryIntoNextZoom() {
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
        OrbitState orbit = new OrbitState(width * height);
        Arrays.fill(pixels, 0xff0a0a0a);
        AtomicInteger done = new AtomicInteger();
        assertTrue(ParallelStepRenderer.fillStep(
                pixels, width, height, 1, scale, 0, 0,
                ops, palette, false, pass1,
                workers, null, done, width * height, null, interior, orbit));
        int maxReached = AdaptiveRefiner.refine(
                pixels, interior, width, height, scale, 0, 0,
                ops, palette, false, pass1, 8, cap,
                workers, null, done, width * height * 4, null, null, 0, orbit);
        assertTrue(maxReached >= pass1);
        assertTrue(maxReached <= cap);
        // Carried pass-1 for the next zoom must be at least this high.
        assertTrue(maxReached > pass1);
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
