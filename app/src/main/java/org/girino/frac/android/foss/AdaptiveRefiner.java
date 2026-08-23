package org.girino.frac.android.foss;

import org.girino.frac.operators.Complex;
import org.girino.frac.operators.FractalOperator;
import org.girino.frac.palettes.PaletteProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Border-doubling adaptive iteration after a full-resolution pass (issue #28).
 * Interior pixels (did not escape at the current limit) that touch an escaped
 * neighbor are re-tested at 2× the limit until none escape or caps are hit.
 */
public final class AdaptiveRefiner {

    private AdaptiveRefiner() {
    }

    /**
     * Refines pixels and interior[] in place. interior[i] true means the
     * sample has not escaped at the limit last applied to that pixel.
     * Returns false if cancelled before finishing.
     */
    public static boolean refine(
            int[] pixels,
            boolean[] interior,
            int width,
            int height,
            double scale,
            double centerX,
            double centerY,
            FractalOperator[] workerOperators,
            PaletteProvider palette,
            boolean smooth,
            int pass1MaxIter,
            int maxRounds,
            int absoluteCap,
            ExecutorService workers,
            ParallelStepRenderer.CancelCheck cancel,
            AtomicInteger doneSamples,
            int progressTotal,
            ParallelStepRenderer.ProgressListener progress) {
        if (pixels == null || interior == null || width <= 0 || height <= 0) {
            return false;
        }
        if (palette == null || workerOperators == null || workerOperators.length == 0) {
            return false;
        }
        if (cancel != null && cancel.isCancelled()) {
            return false;
        }

        int currentLimit = Math.max(pass1MaxIter, IterationSettings.MIN_ITER);
        int cap = Math.max(absoluteCap, currentLimit);
        int roundsLimit = Math.max(1, maxRounds);
        int[] border = new int[width * height];

        for (int round = 0; round < roundsLimit; round++) {
            if (cancel != null && cancel.isCancelled()) {
                return false;
            }
            int nextLimit = currentLimit * 2;
            if (nextLimit > cap) {
                nextLimit = cap;
            }
            if (nextLimit <= currentLimit) {
                break;
            }

            int borderCount = collectBorder(interior, width, height, border);
            if (borderCount == 0) {
                break;
            }

            AtomicBoolean anyEscaped = new AtomicBoolean(false);
            boolean finished = retestBorder(
                    pixels, interior, border, borderCount,
                    width, height, scale, centerX, centerY,
                    workerOperators, palette, smooth, nextLimit,
                    workers, cancel, anyEscaped,
                    doneSamples, progressTotal, progress);
            if (!finished) {
                return false;
            }
            if (!anyEscaped.get()) {
                break;
            }
            currentLimit = nextLimit;
        }
        return cancel == null || !cancel.isCancelled();
    }

    /**
     * Marks 4-connected interior-vs-escaped border indices into borderOut.
     * Returns the count of border pixels.
     */
    static int collectBorder(boolean[] interior, int width, int height, int[] borderOut) {
        int count = 0;
        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                int i = row + x;
                if (!interior[i]) {
                    continue;
                }
                if (hasEscapedNeighbor(interior, width, height, x, y)) {
                    borderOut[count++] = i;
                }
            }
        }
        return count;
    }

    static boolean hasEscapedNeighbor(boolean[] interior, int width, int height, int x, int y) {
        if (x > 0 && !interior[y * width + (x - 1)]) {
            return true;
        }
        if (x + 1 < width && !interior[y * width + (x + 1)]) {
            return true;
        }
        if (y > 0 && !interior[(y - 1) * width + x]) {
            return true;
        }
        if (y + 1 < height && !interior[(y + 1) * width + x]) {
            return true;
        }
        return false;
    }

    private static boolean retestBorder(
            int[] pixels,
            boolean[] interior,
            int[] border,
            int borderCount,
            int width,
            int height,
            double scale,
            double centerX,
            double centerY,
            FractalOperator[] workerOperators,
            PaletteProvider palette,
            boolean smooth,
            int nextLimit,
            ExecutorService workers,
            ParallelStepRenderer.CancelCheck cancel,
            AtomicBoolean anyEscaped,
            AtomicInteger doneSamples,
            int progressTotal,
            ParallelStepRenderer.ProgressListener progress) {
        int workerCount = workerOperators.length;
        int tasks = workers != null ? Math.min(workerCount, borderCount) : 1;
        if (tasks <= 1 || workers == null) {
            return retestRange(
                    pixels, interior, border, 0, borderCount,
                    width, height, scale, centerX, centerY,
                    workerOperators[0], palette, smooth, nextLimit,
                    cancel, anyEscaped, doneSamples, progressTotal, progress);
        }

        AtomicBoolean cancelled = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(tasks);
        List<Future<?>> futures = new ArrayList<>(tasks);
        for (int t = 0; t < tasks; t++) {
            final int from = t * borderCount / tasks;
            final int to = (t + 1) * borderCount / tasks;
            final FractalOperator op = workerOperators[t];
            futures.add(workers.submit(() -> {
                try {
                    if (!retestRange(
                            pixels, interior, border, from, to,
                            width, height, scale, centerX, centerY,
                            op, palette, smooth, nextLimit,
                            cancel, anyEscaped, doneSamples, progressTotal, progress)) {
                        cancelled.set(true);
                    }
                } finally {
                    latch.countDown();
                }
            }));
        }

        try {
            while (true) {
                if (cancel != null && cancel.isCancelled()) {
                    cancelled.set(true);
                    for (Future<?> future : futures) {
                        future.cancel(true);
                    }
                    latch.await();
                    return false;
                }
                if (latch.await(50, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            cancelled.set(true);
            for (Future<?> future : futures) {
                future.cancel(true);
            }
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

    private static boolean retestRange(
            int[] pixels,
            boolean[] interior,
            int[] border,
            int from,
            int to,
            int width,
            int height,
            double scale,
            double centerX,
            double centerY,
            FractalOperator operator,
            PaletteProvider palette,
            boolean smooth,
            int nextLimit,
            ParallelStepRenderer.CancelCheck cancel,
            AtomicBoolean anyEscaped,
            AtomicInteger doneSamples,
            int progressTotal,
            ParallelStepRenderer.ProgressListener progress) {
        Complex point = new Complex();
        final int reportEvery = Math.max(1, Math.max(1, progressTotal) / 100);
        for (int b = from; b < to; b++) {
            if (Thread.currentThread().isInterrupted()
                    || (cancel != null && cancel.isCancelled())) {
                return false;
            }
            int index = border[b];
            int x = index % width;
            int y = index / width;
            point.set(
                    (x - width / 2.0) / scale + centerX,
                    (y - height / 2.0) / scale + centerY);
            FractalOperator.EscapeSample sample = operator.sample(point, nextLimit, smooth);
            pixels[index] = palette.getColor(sample.value);
            if (sample.escaped) {
                interior[index] = false;
                anyEscaped.set(true);
            } else {
                interior[index] = true;
            }
            if (doneSamples != null) {
                int completed = doneSamples.incrementAndGet();
                if (progress != null && completed % reportEvery == 0) {
                    progress.onProgress(completed, progressTotal);
                }
            }
        }
        return true;
    }
}
