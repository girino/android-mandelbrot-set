package org.girino.frac.android.foss;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
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

/** Parallel progressive step fill (issue #25). */
public class ParallelStepRendererTest {

    private ExecutorService workers;

    @Before
    public void setUp() {
        workers = Executors.newFixedThreadPool(4);
    }

    @After
    public void tearDown() {
        workers.shutdownNow();
    }

    @Test
    public void defaultWorkerCount_isBetweenOneAndEight() {
        int n = ParallelStepRenderer.defaultWorkerCount();
        assertTrue(n >= 1);
        assertTrue(n <= 8);
    }

    @Test
    public void adaptiveWorkerCount_isAtLeastDefault_andCappedAtSixteen() {
        int adaptive = ParallelStepRenderer.adaptiveWorkerCount();
        int step = ParallelStepRenderer.defaultWorkerCount();
        assertTrue(adaptive >= step);
        assertTrue(adaptive >= 1);
        assertTrue(adaptive <= 16);
    }

    @Test
    public void fillBlock_paintsStepRect() {
        int[] pixels = new int[4 * 4];
        ParallelStepRenderer.fillBlock(pixels, 4, 4, 1, 1, 2, 0xFF112233);
        assertEquals(0xFF112233, pixels[1 * 4 + 1]);
        assertEquals(0xFF112233, pixels[1 * 4 + 2]);
        assertEquals(0xFF112233, pixels[2 * 4 + 1]);
        assertEquals(0xFF112233, pixels[2 * 4 + 2]);
        assertEquals(0, pixels[0]);
    }

    @Test
    public void parallelAndSerial_matchForCoarseSteps() {
        int width = 64;
        int height = 48;
        double scale = 100.0 * 300.0 / width;
        PaletteProvider palette = new HSBPalette();
        int maxIter = 40;

        for (int step : new int[] {8, 4, 2}) {
            int[] serial = new int[width * height];
            Arrays.fill(serial, 0xff0a0a0a);
            AtomicInteger serialDone = new AtomicInteger();
            FractalOperator[] one = {new OptimizedMandelbrotOperator()};
            assertTrue(ParallelStepRenderer.fillStep(
                    serial, width, height, step, scale, 0, 0,
                    one, palette, false, maxIter,
                    null, null, serialDone, countSamples(width, height, step), null));

            int[] parallel = new int[width * height];
            Arrays.fill(parallel, 0xff0a0a0a);
            AtomicInteger parallelDone = new AtomicInteger();
            FractalOperator[] many = {
                    new OptimizedMandelbrotOperator(),
                    new OptimizedMandelbrotOperator(),
                    new OptimizedMandelbrotOperator(),
                    new OptimizedMandelbrotOperator()
            };
            assertTrue(ParallelStepRenderer.fillStep(
                    parallel, width, height, step, scale, 0, 0,
                    many, palette, false, maxIter,
                    workers, null, parallelDone, countSamples(width, height, step), null));

            assertEquals(serialDone.get(), parallelDone.get());
            assertArrayEquals(serial, parallel);
        }
    }

    private static int countSamples(int width, int height, int step) {
        return ((width - 1) / step + 1) * ((height - 1) / step + 1);
    }

    @Test
    public void parallelAndSerial_matchForSmallViewport() {
        int width = 64;
        int height = 48;
        int step = 1;
        double scale = 100.0 * 300.0 / width;
        PaletteProvider palette = new HSBPalette();
        int maxIter = 40;

        int[] serial = new int[width * height];
        Arrays.fill(serial, 0xff0a0a0a);
        AtomicInteger serialDone = new AtomicInteger();
        FractalOperator[] one = {new OptimizedMandelbrotOperator()};
        assertTrue(ParallelStepRenderer.fillStep(
                serial, width, height, step, scale, 0, 0,
                one, palette, false, maxIter,
                null, null, serialDone, width * height, null));

        int[] parallel = new int[width * height];
        Arrays.fill(parallel, 0xff0a0a0a);
        AtomicInteger parallelDone = new AtomicInteger();
        FractalOperator[] many = {
                new OptimizedMandelbrotOperator(),
                new OptimizedMandelbrotOperator(),
                new OptimizedMandelbrotOperator(),
                new OptimizedMandelbrotOperator()
        };
        assertTrue(ParallelStepRenderer.fillStep(
                parallel, width, height, step, scale, 0, 0,
                many, palette, false, maxIter,
                workers, null, parallelDone, width * height, null));

        assertEquals(serialDone.get(), parallelDone.get());
        assertArrayEquals(serial, parallel);
    }

    @Test
    public void fillStep_stopsWhenCancelled() {
        int width = 120;
        int height = 120;
        AtomicInteger checks = new AtomicInteger();
        AtomicInteger done = new AtomicInteger();
        FractalOperator[] ops = {
                new OptimizedMandelbrotOperator(),
                new OptimizedMandelbrotOperator()
        };
        int[] pixels = new int[width * height];
        Arrays.fill(pixels, 0xff0a0a0a);
        boolean finished = ParallelStepRenderer.fillStep(
                pixels, width, height, 1, 50, 0, 0,
                ops, new HSBPalette(), false, 200,
                workers,
                () -> checks.incrementAndGet() > 8,
                done,
                width * height,
                null);
        assertFalse(finished);
        assertTrue(done.get() < width * height);
    }

    @Test
    public void formulaCatalog_createLike_isFreshInstance() {
        FractalOperator a = FormulaCatalog.create(0);
        FractalOperator b = FormulaCatalog.createLike(a);
        assertNotSame(a, b);
        assertEquals(OptimizedMandelbrotOperator.class, b.getClass());
    }
}
