package org.girino.frac.android.foss;

import org.girino.frac.operators.Complex;
import org.girino.frac.operators.FractalOperator;
import org.girino.frac.palettes.PaletteProvider;
import org.girino.frac.viewport.ViewportTransforms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Parallel fill of one progressive render step into an ARGB pixel buffer
 * (issue #25). Rows are partitioned across workers; each worker uses its
 * own FractalOperator instance.
 */
public final class ParallelStepRenderer {

    public interface CancelCheck {
        boolean isCancelled();
    }

    public interface ProgressListener {
        void onProgress(int completed, int total);
    }

    private ParallelStepRenderer() {
    }

    /**
     * Default worker count: available processors, at least 1, at most 8.
     * (Issue text suggested 4x cores; that oversubscribes on phones.)
     */
    public static int defaultWorkerCount() {
        int cores = Runtime.getRuntime().availableProcessors();
        return Math.max(1, Math.min(8, cores));
    }

    /**
     * Higher worker count for Adaptive border retests: 2× cores, capped at 16.
     * Pass-1 stays on defaultWorkerCount to avoid oversubscribing coarse steps.
     */
    public static int adaptiveWorkerCount() {
        int cores = Runtime.getRuntime().availableProcessors();
        return Math.max(1, Math.min(16, cores * 2));
    }

    /** Shared factory for fractal sample worker threads (daemon so tests can exit). */
    public static ThreadFactory workerThreadFactory(String namePrefix) {
        return new ThreadFactory() {
            private final AtomicInteger next = new AtomicInteger();

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, namePrefix + next.getAndIncrement());
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                return thread;
            }
        };
    }

    /**
     * Fills pixels for one progressive step (8, 4, 2, or 1). Returns false
     * if cancelled before the step completed.
     */
    public static boolean fillStep(
            int[] pixels,
            int width,
            int height,
            int step,
            double scale,
            double centerX,
            double centerY,
            FractalOperator[] workerOperators,
            PaletteProvider palette,
            boolean smooth,
            int maxIter,
            ExecutorService workers,
            CancelCheck cancel,
            AtomicInteger doneSamples,
            int totalSamples,
            ProgressListener progress) {
        return fillStep(
                pixels, width, height, step, scale, centerX, centerY,
                workerOperators, palette, smooth, maxIter,
                workers, cancel, doneSamples, totalSamples, progress, null);
    }

    /**
     * Like fillStep, and when interior is non-null and step is 1, records
     * per-pixel interior (true = did not escape at maxIter) for adaptive
     * refinement (issue #28).
     */
    public static boolean fillStep(
            int[] pixels,
            int width,
            int height,
            int step,
            double scale,
            double centerX,
            double centerY,
            FractalOperator[] workerOperators,
            PaletteProvider palette,
            boolean smooth,
            int maxIter,
            ExecutorService workers,
            CancelCheck cancel,
            AtomicInteger doneSamples,
            int totalSamples,
            ProgressListener progress,
            boolean[] interior) {
        return fillStep(
                pixels, width, height, step, scale, centerX, centerY,
                workerOperators, palette, smooth, maxIter,
                workers, cancel, doneSamples, totalSamples, progress, interior, null);
    }

    /**
     * Like fillStep with interior; when orbit is non-null and step is 1,
     * stores per-pixel orbit checkpoints for Adaptive warm-start.
     */
    public static boolean fillStep(
            int[] pixels,
            int width,
            int height,
            int step,
            double scale,
            double centerX,
            double centerY,
            FractalOperator[] workerOperators,
            PaletteProvider palette,
            boolean smooth,
            int maxIter,
            ExecutorService workers,
            CancelCheck cancel,
            AtomicInteger doneSamples,
            int totalSamples,
            ProgressListener progress,
            boolean[] interior,
            OrbitState orbit) {
        return fillStep(
                pixels, width, height, step, scale, centerX, centerY,
                workerOperators, palette, smooth, maxIter,
                workers, cancel, doneSamples, totalSamples, progress, interior, orbit, null);
    }

    /**
     * Like fillStep with interior and orbit; when anyEscapedOut is non-null,
     * set to true if any sampled pixel escaped at maxIter (issue #48).
     */
    public static boolean fillStep(
            int[] pixels,
            int width,
            int height,
            int step,
            double scale,
            double centerX,
            double centerY,
            FractalOperator[] workerOperators,
            PaletteProvider palette,
            boolean smooth,
            int maxIter,
            ExecutorService workers,
            CancelCheck cancel,
            AtomicInteger doneSamples,
            int totalSamples,
            ProgressListener progress,
            boolean[] interior,
            OrbitState orbit,
            AtomicBoolean anyEscapedOut) {
        if (pixels == null || width <= 0 || height <= 0 || step <= 0) {
            return false;
        }
        if (cancel != null && cancel.isCancelled()) {
            return false;
        }
        if (anyEscapedOut != null) {
            anyEscapedOut.set(false);
        }

        int rowCount = (height - 1) / step + 1;
        int workerCount = workerOperators != null ? workerOperators.length : 0;
        if (workerCount <= 0 || workers == null) {
            return fillStepSerial(
                    pixels, width, height, step, scale, centerX, centerY,
                    workerOperators != null && workerOperators.length > 0
                            ? workerOperators[0]
                            : null,
                    palette, smooth, maxIter, cancel, doneSamples, totalSamples, progress,
                    interior, orbit, anyEscapedOut);
        }

        int tasks = Math.min(workerCount, rowCount);
        if (tasks <= 1) {
            return fillStepSerial(
                    pixels, width, height, step, scale, centerX, centerY,
                    workerOperators[0], palette, smooth, maxIter,
                    cancel, doneSamples, totalSamples, progress, interior, orbit, anyEscapedOut);
        }

        final int reportEvery = Math.max(1, totalSamples / 100);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(tasks);
        List<Future<?>> futures = new ArrayList<>(tasks);

        for (int t = 0; t < tasks; t++) {
            final int rowStart = t * rowCount / tasks;
            final int rowEnd = (t + 1) * rowCount / tasks;
            final FractalOperator operator = workerOperators[t];
            futures.add(workers.submit(() -> {
                try {
                    fillRowRange(
                            pixels, width, height, step, scale, centerX, centerY,
                            operator, palette, smooth, maxIter,
                            rowStart, rowEnd, cancel, cancelled,
                            doneSamples, totalSamples, reportEvery, progress, interior, orbit,
                            anyEscapedOut);
                } finally {
                    latch.countDown();
                }
            }));
        }

        try {
            while (true) {
                if (cancel != null && cancel.isCancelled()) {
                    cancelled.set(true);
                    cancelAll(futures);
                    latch.await();
                    return false;
                }
                if (latch.await(50, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            cancelled.set(true);
            cancelAll(futures);
            Thread.currentThread().interrupt();
            try {
                latch.await();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return false;
        }

        return !cancelled.get() && (cancel == null || !cancel.isCancelled());
    }

    private static void cancelAll(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            future.cancel(true);
        }
    }

    private static boolean fillStepSerial(
            int[] pixels,
            int width,
            int height,
            int step,
            double scale,
            double centerX,
            double centerY,
            FractalOperator operator,
            PaletteProvider palette,
            boolean smooth,
            int maxIter,
            CancelCheck cancel,
            AtomicInteger doneSamples,
            int totalSamples,
            ProgressListener progress,
            boolean[] interior,
            OrbitState orbit,
            AtomicBoolean anyEscapedOut) {
        if (operator == null) {
            return false;
        }
        AtomicBoolean cancelled = new AtomicBoolean(false);
        int rowCount = (height - 1) / step + 1;
        final int reportEvery = Math.max(1, totalSamples / 100);
        fillRowRange(
                pixels, width, height, step, scale, centerX, centerY,
                operator, palette, smooth, maxIter,
                0, rowCount, cancel, cancelled,
                doneSamples, totalSamples, reportEvery, progress, interior, orbit,
                anyEscapedOut);
        return !cancelled.get() && (cancel == null || !cancel.isCancelled());
    }

    private static void fillRowRange(
            int[] pixels,
            int width,
            int height,
            int step,
            double scale,
            double centerX,
            double centerY,
            FractalOperator operator,
            PaletteProvider palette,
            boolean smooth,
            int maxIter,
            int rowStart,
            int rowEnd,
            CancelCheck cancel,
            AtomicBoolean cancelled,
            AtomicInteger doneSamples,
            int totalSamples,
            int reportEvery,
            ProgressListener progress,
            boolean[] interior,
            OrbitState orbit,
            AtomicBoolean anyEscapedOut) {
        Complex point = new Complex();
        Complex orbitScratch = orbit != null ? new Complex() : null;
        // Issue #33: one EscapeSample per worker row band (see FractalOperator.EscapeSample).
        FractalOperator.EscapeSample sample = new FractalOperator.EscapeSample();
        for (int row = rowStart; row < rowEnd; row++) {
            if (Thread.currentThread().isInterrupted()
                    || cancelled.get()
                    || (cancel != null && cancel.isCancelled())) {
                cancelled.set(true);
                return;
            }
            int y = row * step;
            double cIm = ViewportTransforms.complexY(y, height, centerY, scale);
            double cRe = ViewportTransforms.complexX(0, width, centerX, scale);
            double cReStep = step / scale;
            for (int x = 0; x < width; x += step) {
                if (Thread.currentThread().isInterrupted()
                        || cancelled.get()
                        || (cancel != null && cancel.isCancelled())) {
                    cancelled.set(true);
                    return;
                }
                point.set(cRe, cIm);
                operator.sampleInto(point, maxIter, smooth, sample);
                if (sample.escaped && anyEscapedOut != null) {
                    anyEscapedOut.set(true);
                }
                int color = palette.getColor(sample.value);
                fillBlock(pixels, width, height, x, y, step, color);
                if (interior != null && step == 1) {
                    int index = y * width + x;
                    interior[index] = !sample.escaped;
                    if (orbit != null) {
                        orbit.storeInterior(index, operator, sample, orbitScratch);
                    }
                }
                int completed = doneSamples.incrementAndGet();
                if (progress != null && completed % reportEvery == 0) {
                    progress.onProgress(completed, totalSamples);
                }
                cRe += cReStep;
            }
        }
    }

    /**
     * Paints a step-by-step block (or a single pixel when step is 1).
     * Issue #37: row segments use Arrays.fill when the block is wider than one pixel.
     */
    static void fillBlock(int[] pixels, int width, int height, int x, int y, int step, int color) {
        int xMax = Math.min(x + step, width);
        int yMax = Math.min(y + step, height);
        int span = xMax - x;
        if (span <= 0 || yMax <= y) {
            return;
        }
        if (span == 1) {
            for (int py = y; py < yMax; py++) {
                pixels[py * width + x] = color;
            }
            return;
        }
        for (int py = y; py < yMax; py++) {
            int rowOffset = py * width + x;
            Arrays.fill(pixels, rowOffset, rowOffset + span, color);
        }
    }
}
