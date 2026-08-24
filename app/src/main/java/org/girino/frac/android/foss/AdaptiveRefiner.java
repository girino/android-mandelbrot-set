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

    /**
     * Throttled snapshot of the pixel buffer while border retest workers run.
     * Invoked on the render thread between worker batches (not per pixel).
     */
    public interface PreviewListener {
        void onPreview(int[] pixels, int width, int height, int currentLimit);
    }

    /** Publish an in-progress frame after this many border samples (approx). */
    static final int PREVIEW_PIXEL_INTERVAL = 4000;
    /** Minimum milliseconds between in-progress publishes. */
    static final long PREVIEW_MIN_INTERVAL_MS = 250L;

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
                workers, cancel, doneSamples, progressTotal, progress, null, 0);
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
                workers, cancel, doneSamples, progressTotal, progress, roundListener, 0, null, null);
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
            int seedMinStopIter) {
        return refine(
                pixels, interior, width, height, scale, centerX, centerY,
                workerOperators, palette, smooth, pass1MaxIter, maxRounds, absoluteCap,
                workers, cancel, doneSamples, progressTotal, progress, roundListener,
                seedMinStopIter, null, null);
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
            int seedMinStopIter,
            OrbitState orbit) {
        return refine(
                pixels, interior, width, height, scale, centerX, centerY,
                workerOperators, palette, smooth, pass1MaxIter, maxRounds, absoluteCap,
                workers, cancel, doneSamples, progressTotal, progress, roundListener,
                seedMinStopIter, orbit, null);
    }

    /**
     * @param seedMinStopIter minimum iteration limit before an empty border
     *         pass may stop doubling (usually the Adaptive value shown on the
     *         overlay from the previous zoom; 0 if none). Doubling always
     *         starts at pass1MaxIter; early stop only when probed limit is
     *         already >= seedMinStopIter (so outer borders keep intermediate
     *         colors). Every border collect unions the image perimeter with
     *         any fractal seam.
     * @param orbit optional per-pixel checkpoints from pass-1; when set,
     *         border retests continue from the stored iteration and Z.
     * @param previewListener optional throttled UI snapshot while retesting.
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
            int seedMinStopIter,
            OrbitState orbit,
            PreviewListener previewListener) {
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
        // Floor from previous overlay Adaptive value: do not early-stop below
        // this even if a double finds no new escapes.
        int minStopLimit = seedMinStopIter > 0 ? Math.min(seedMinStopIter, cap) : 0;
        int roundsLimit = Math.max(1, maxRounds);
        int[] border = new int[width * height];
        int[] frameScratch = new int[width * height];
        boolean[] onBorder = new boolean[width * height];
        AtomicInteger previewSamples = previewListener != null ? new AtomicInteger(0) : null;
        int workerCount = workerOperators.length;

        int round = 0;
        while (true) {
            if (cancel != null && cancel.isCancelled()) {
                return -1;
            }
            boolean underFloor = currentLimit < minStopLimit;
            // maxRounds applies only once we have reached the previous-zoom floor;
            // while under the floor we keep doubling regardless of round count.
            if (round >= roundsLimit && !underFloor) {
                break;
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

            // Pixels already sampled at nextLimit are skipped on later
            // stabilize passes. Cleared when the iteration cap doubles.
            boolean[] visitedAtLimit = new boolean[width * height];

            // Stabilize at nextLimit: re-collect border and retest until a
            // full pass finds no new escapes (no new border filled).
            // Screen-edge perimeter is always part of the border.
            boolean anyEscapedAtThisLimit = false;
            while (true) {
                if (cancel != null && cancel.isCancelled()) {
                    return -1;
                }
                int borderCount = collectBorder(
                        interior, width, height, border, true, frameScratch, onBorder,
                        workers, workerCount, cancel);
                if (borderCount == 0) {
                    if (cancel != null && cancel.isCancelled()) {
                        return -1;
                    }
                    // Nothing left to refine; cannot climb further toward floor.
                    return currentLimit;
                }
                borderCount = dropVisitedBorder(border, borderCount, visitedAtLimit);
                if (borderCount == 0) {
                    // Geometric border remains, but every candidate was already
                    // probed at this limit — nothing new can escape until we double.
                    break;
                }

                AtomicBoolean anyEscaped = new AtomicBoolean(false);
                boolean finished = retestBorder(
                        pixels, interior, border, borderCount,
                        width, height, scale, centerX, centerY,
                        workerOperators, palette, smooth, nextLimit,
                        workers, cancel, anyEscaped,
                        doneSamples, progressTotal, progress, orbit,
                        previewListener, previewSamples, nextLimit,
                        visitedAtLimit);
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

            currentLimit = nextLimit;
            round++;

            // Stop only when no new border escapes AND we have reached the
            // previous zoom's min-stop floor (or there was no previous floor).
            if (!anyEscapedAtThisLimit) {
                if (roundListener != null && currentLimit < minStopLimit) {
                    // Floor-only climb: refresh Iter on the overlay even though
                    // pixels did not change.
                    roundListener.onRoundComplete(pixels, width, height, currentLimit);
                }
                if (currentLimit >= minStopLimit) {
                    break;
                }
            }
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
     * Parallel seam scan when workers are available; falls back to serial.
     */
    static int collectBorder(
            boolean[] interior,
            int width,
            int height,
            int[] borderOut,
            boolean alsoFrame,
            int[] frameScratch,
            boolean[] onBorder,
            ExecutorService workers,
            int workerCount,
            ParallelStepRenderer.CancelCheck cancel) {
        if (workers == null || workerCount <= 1 || height <= 1) {
            return collectBorder(interior, width, height, borderOut, alsoFrame, frameScratch, onBorder);
        }
        AtomicInteger count = new AtomicInteger(0);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        int tasks = Math.min(workerCount, height);
        CountDownLatch latch = new CountDownLatch(tasks);
        List<Future<?>> futures = new ArrayList<>(tasks);
        for (int t = 0; t < tasks; t++) {
            final int yStart = t * height / tasks;
            final int yEnd = (t + 1) * height / tasks;
            futures.add(workers.submit(() -> {
                try {
                    for (int y = yStart; y < yEnd; y++) {
                        if (cancelled.get()
                                || Thread.currentThread().isInterrupted()
                                || (cancel != null && cancel.isCancelled())) {
                            cancelled.set(true);
                            return;
                        }
                        int row = y * width;
                        for (int x = 0; x < width; x++) {
                            int i = row + x;
                            if (!interior[i]) {
                                continue;
                            }
                            if (hasEscapedNeighbor(interior, width, height, x, y)) {
                                int slot = count.getAndIncrement();
                                borderOut[slot] = i;
                            }
                        }
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
                    return 0;
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
            return 0;
        }
        if (cancelled.get() || (cancel != null && cancel.isCancelled())) {
            return 0;
        }
        int seamCount = count.get();
        if (seamCount == 0) {
            return collectFrameInterior(interior, width, height, borderOut);
        }
        if (!alsoFrame) {
            return seamCount;
        }
        if (frameScratch == null || onBorder == null) {
            frameScratch = new int[width * height];
            onBorder = new boolean[width * height];
        } else {
            Arrays.fill(onBorder, false);
        }
        for (int i = 0; i < seamCount; i++) {
            onBorder[borderOut[i]] = true;
        }
        int frameCount = collectFrameInterior(interior, width, height, frameScratch);
        for (int i = 0; i < frameCount; i++) {
            int idx = frameScratch[i];
            if (!onBorder[idx]) {
                borderOut[seamCount++] = idx;
                onBorder[idx] = true;
            }
        }
        return seamCount;
    }

    /**
     * Drops indices already probed at the current iteration limit so
     * stabilize passes only sample newly exposed border pixels.
     */
    static int dropVisitedBorder(int[] border, int borderCount, boolean[] visitedAtLimit) {
        if (border == null || visitedAtLimit == null || borderCount <= 0) {
            return Math.max(0, borderCount);
        }
        int kept = 0;
        for (int i = 0; i < borderCount; i++) {
            int idx = border[i];
            if (idx < 0 || idx >= visitedAtLimit.length || visitedAtLimit[idx]) {
                continue;
            }
            border[kept++] = idx;
        }
        return kept;
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
            ParallelStepRenderer.ProgressListener progress,
            OrbitState orbit,
            PreviewListener previewListener,
            AtomicInteger previewSamples,
            int previewLimit,
            boolean[] visitedAtLimit) {
        int workerCount = workerOperators.length;
        int tasks = workers != null ? Math.min(workerCount, borderCount) : 1;
        if (tasks <= 1 || workers == null) {
            boolean ok = retestRange(
                    pixels, interior, border, 0, borderCount,
                    width, height, scale, centerX, centerY,
                    workerOperators[0], palette, smooth, nextLimit,
                    cancel, anyEscaped, doneSamples, progressTotal, progress, orbit,
                    previewSamples, visitedAtLimit);
            if (ok && previewListener != null) {
                previewListener.onPreview(pixels, width, height, previewLimit);
            }
            return ok;
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
                            cancel, anyEscaped, doneSamples, progressTotal, progress, orbit,
                            previewSamples, visitedAtLimit)) {
                        cancelled.set(true);
                    }
                } finally {
                    latch.countDown();
                }
            }));
        }

        int lastPreviewAt = previewSamples != null ? previewSamples.get() : 0;
        long lastPreviewMs = System.currentTimeMillis();
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
                if (previewListener != null && previewSamples != null) {
                    int done = previewSamples.get();
                    long now = System.currentTimeMillis();
                    if (done - lastPreviewAt >= PREVIEW_PIXEL_INTERVAL
                            || now - lastPreviewMs >= PREVIEW_MIN_INTERVAL_MS) {
                        previewListener.onPreview(pixels, width, height, previewLimit);
                        lastPreviewAt = done;
                        lastPreviewMs = now;
                    }
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
        if (!cancelled.get() && previewListener != null) {
            previewListener.onPreview(pixels, width, height, previewLimit);
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
            ParallelStepRenderer.ProgressListener progress,
            OrbitState orbit,
            AtomicInteger previewSamples,
            boolean[] visitedAtLimit) {
        Complex point = new Complex();
        Complex orbitScratch = orbit != null ? new Complex() : null;
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
                    ViewportTransforms.complexX(x, width, centerX, scale),
                    ViewportTransforms.complexY(y, height, centerY, scale));
            FractalOperator.EscapeSample sample;
            if (orbit != null && orbit.hasCheckpoint(index)) {
                sample = operator.sampleContinue(
                        point,
                        orbit.iter[index],
                        orbit.re[index],
                        orbit.im[index],
                        nextLimit,
                        smooth);
            } else {
                sample = operator.sample(point, nextLimit, smooth);
            }
            if (visitedAtLimit != null) {
                visitedAtLimit[index] = true;
            }
            pixels[index] = palette.getColor(sample.value);
            if (sample.escaped) {
                interior[index] = false;
                anyEscaped.set(true);
                if (orbit != null) {
                    orbit.clear(index);
                }
            } else {
                interior[index] = true;
                if (orbit != null) {
                    orbit.storeInterior(index, operator, sample, orbitScratch);
                }
            }
            if (doneSamples != null) {
                int completed = doneSamples.incrementAndGet();
                if (progress != null && completed % reportEvery == 0) {
                    progress.onProgress(completed, progressTotal);
                }
            }
            if (previewSamples != null) {
                previewSamples.incrementAndGet();
            }
        }
        return true;
    }
}
