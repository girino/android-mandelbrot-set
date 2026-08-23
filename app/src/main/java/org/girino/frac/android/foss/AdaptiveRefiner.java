package org.girino.frac.android.foss;

import org.girino.frac.operators.Complex;
import org.girino.frac.operators.FractalOperator;
import org.girino.frac.palettes.PaletteProvider;

import java.util.ArrayList;
import java.util.Arrays;
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
                workers, cancel, doneSamples, progressTotal, progress, null, 0, false);
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
        return refine(
                pixels, interior, width, height, scale, centerX, centerY,
                workerOperators, palette, smooth, pass1MaxIter, maxRounds, absoluteCap,
                workers, cancel, doneSamples, progressTotal, progress, roundListener, 0, false);
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
            RoundListener roundListener,
            int seedMaxIter) {
        return refine(
                pixels, interior, width, height, scale, centerX, centerY,
                workerOperators, palette, smooth, pass1MaxIter, maxRounds, absoluteCap,
                workers, cancel, doneSamples, progressTotal, progress, roundListener,
                seedMaxIter, false);
    }

    /**
     * @param seedMaxIter when greater than pass1MaxIter and the frame needs a
     *         screen-edge seed (all interior, or zoomOutBoost), start doubling
     *         from seedMaxIter (last Adaptive max) instead of pass1.
     * @param zoomOutBoost on zoom-out, always include the image perimeter in
     *         the border set and allow seedMaxIter even if a fractal seam
     *         already exists (zoom-in all-black still uses needsFrameSeed).
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
            ParallelStepRenderer.ProgressListener progress,
            RoundListener roundListener,
            int seedMaxIter,
            boolean zoomOutBoost) {
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
        boolean frameSeed = needsFrameSeed(interior, width, height);
        if (seedMaxIter > currentLimit && (frameSeed || zoomOutBoost)) {
            currentLimit = Math.min(seedMaxIter, cap);
        }
        int roundsLimit = Math.max(1, maxRounds);
        int[] border = new int[width * height];
        int[] frameScratch = zoomOutBoost ? new int[width * height] : null;
        boolean[] onBorder = zoomOutBoost ? new boolean[width * height] : null;

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
                int borderCount = collectBorder(
                        interior, width, height, border, zoomOutBoost, frameScratch, onBorder);
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
     * True when some pixels are still interior and there is no
     * interior/exterior 4-connected seam — Adaptive will seed from the
     * image frame instead.
     */
    static boolean needsFrameSeed(boolean[] interior, int width, int height) {
        if (interior == null || width <= 0 || height <= 0) {
            return false;
        }
        boolean anyInterior = false;
        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                int i = row + x;
                if (!interior[i]) {
                    continue;
                }
                anyInterior = true;
                if (hasEscapedNeighbor(interior, width, height, x, y)) {
                    return false;
                }
            }
        }
        return anyInterior;
    }

    /**
     * Marks 4-connected interior-vs-escaped border indices into borderOut.
     * When the whole frame is still interior (no escaped neighbor anywhere —
     * the "all black" case), uses the image perimeter as the seed border so
     * doubling can still probe from the edges inward.
     * Returns the count of border pixels (0 only when nothing remains interior).
     */
    static int collectBorder(boolean[] interior, int width, int height, int[] borderOut) {
        return collectBorder(interior, width, height, borderOut, false, null, null);
    }

    /**
     * @param alsoFrame when true, union the image-perimeter interior pixels
     *         with any fractal seam (zoom-out boost).
     */
    static int collectBorder(
            boolean[] interior,
            int width,
            int height,
            int[] borderOut,
            boolean alsoFrame,
            int[] frameScratch,
            boolean[] onBorder) {
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
        if (count == 0) {
            return collectFrameInterior(interior, width, height, borderOut);
        }
        if (!alsoFrame) {
            return count;
        }
        if (frameScratch == null || onBorder == null) {
            frameScratch = new int[width * height];
            onBorder = new boolean[width * height];
        } else {
            Arrays.fill(onBorder, false);
        }
        for (int i = 0; i < count; i++) {
            onBorder[borderOut[i]] = true;
        }
        int frameCount = collectFrameInterior(interior, width, height, frameScratch);
        for (int i = 0; i < frameCount; i++) {
            int idx = frameScratch[i];
            if (!onBorder[idx]) {
                borderOut[count++] = idx;
                onBorder[idx] = true;
            }
        }
        return count;
    }

    /**
     * Interior pixels on the image edge (top/bottom rows and left/right
     * columns). Used when Adaptive has no interior/exterior seam yet.
     */
    static int collectFrameInterior(boolean[] interior, int width, int height, int[] borderOut) {
        int count = 0;
        if (interior == null || width <= 0 || height <= 0 || borderOut == null) {
            return 0;
        }
        for (int x = 0; x < width; x++) {
            int top = x;
            if (interior[top]) {
                borderOut[count++] = top;
            }
            if (height > 1) {
                int bottom = (height - 1) * width + x;
                if (interior[bottom]) {
                    borderOut[count++] = bottom;
                }
            }
        }
        for (int y = 1; y < height - 1; y++) {
            int left = y * width;
            if (interior[left]) {
                borderOut[count++] = left;
            }
            if (width > 1) {
                int right = left + (width - 1);
                if (interior[right]) {
                    borderOut[count++] = right;
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
