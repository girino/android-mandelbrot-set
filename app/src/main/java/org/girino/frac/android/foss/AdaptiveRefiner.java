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
 *
 * At each doubled limit: keep finding the interior/exterior border and
 * re-testing those pixels until a full pass finds no new escapes ("no new
 * border filled"), then double again — or stop early if the first pass at
 * that limit finds nothing. Caps: maxRounds of doublings and absoluteCap.
 */
public final class AdaptiveRefiner {

    /** Called after pixels change so the UI can show intermediate borders. */
    public interface RoundListener {
        void onRoundComplete(int[] pixels, int width, int height, int currentLimit);
    }

    private AdaptiveRefiner() {
    }

    /**
     * Refines pixels and interior[] in place. interior[i] true means the
     * sample has not escaped at the limit last applied to that pixel.
     *
     * @return max iteration limit reached by border refinement (at least
     *         pass1MaxIter), or -1 if cancelled. Used for overlay display.
     */
    public static int refine(
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
        return refine(
                pixels, interior, width, height, scale, centerX, centerY,
                workerOperators, palette, smooth, pass1MaxIter, maxRounds, absoluteCap,
                workers, cancel, doneSamples, progressTotal, progress, null);
    }

    public static int refine(
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
            ParallelStepRenderer.ProgressListener progress,
            RoundListener roundListener) {
        if (pixels == null || interior == null || width <= 0 || height <= 0) {
            return -1;
        }
        if (palette == null || workerOperators == null || workerOperators.length == 0) {
            return -1;
        }
        if (cancel != null && cancel.isCancelled()) {
            return -1;
        }

        int currentLimit = Math.max(pass1MaxIter, IterationSettings.MIN_ITER);
        int cap = Math.max(absoluteCap, currentLimit);
        int roundsLimit = Math.max(1, maxRounds);
        int[] border = new int[width * height];

        for (int round = 0; round < roundsLimit; round++) {
            if (cancel != null && cancel.isCancelled()) {
                return -1;
            }
            int nextLimit = currentLimit > IterationSettings.MAX_ITER_CAP / 2
                    ? IterationSettings.MAX_ITER_CAP
                    : currentLimit * 2;
            if (nextLimit > cap) {
                nextLimit = cap;
            }
            if (nextLimit <= currentLimit) {
                break;
            }

            // Stabilize at nextLimit: re-collect border and retest until a
            // full pass finds no new escapes (no new border filled).
            boolean anyEscapedAtThisLimit = false;
            while (true) {
                if (cancel != null && cancel.isCancelled()) {
                    return -1;
                }
                int borderCount = collectBorder(interior, width, height, border);
                if (borderCount == 0) {
                    if (cancel != null && cancel.isCancelled()) {
                        return -1;
                    }
                    return currentLimit;
                }

                AtomicBoolean anyEscaped = new AtomicBoolean(false);
                boolean finished = retestBorder(
                        pixels, interior, border, borderCount,
                        width, height, scale, centerX, centerY,
                        workerOperators, palette, smooth, nextLimit,
                        workers, cancel, anyEscaped,
                        doneSamples, progressTotal, progress);
                if (!finished) {
                    return -1;
                }
                if (!anyEscaped.get()) {
                    break;
                }
                anyEscapedAtThisLimit = true;
                if (roundListener != null) {
                    roundListener.onRoundComplete(pixels, width, height, nextLimit);
                }
            }

            // Issue #28: if the first (and only) pass at 2x found no
            // divergence, stop — further doubling of the same border is done.
            if (!anyEscapedAtThisLimit) {
                break;
            }
            currentLimit = nextLimit;
        }
        if (cancel != null && cancel.isCancelled()) {
            return -1;
        }
        return currentLimit;
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
            // Skip pixels that already escaped in an earlier pass of this
            // stabilize loop (border list can be stale across workers only
            // within one pass; within one pass each index appears once).
            if (!interior[index]) {
                continue;
            }
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
