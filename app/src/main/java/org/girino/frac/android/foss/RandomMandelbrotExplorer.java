package org.girino.frac.android.foss;

import org.girino.frac.operators.Complex;
import org.girino.frac.operators.FractalOperator;
import org.girino.frac.operators.OptimizedMandelbrotOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Finds a detailed Mandelbrot viewport by repeatedly sampling its boundary. */
final class RandomMandelbrotExplorer {

    static final int MIN_PASSES = 3;
    static final int MAX_PASSES = 12;
    static final double MIN_ZOOM = 1.5;
    static final double MAX_ZOOM = 4.0;
    static final int SAMPLE_SIZE = 128;
    private static final double INITIAL_CENTER_X = -0.5;
    private static final double INITIAL_VIEW_WIDTH = 3.5;

    interface CancelCheck {
        boolean isCancelled();
    }

    static final class Result {
        final double centerX;
        final double centerY;
        final double viewWidth;

        Result(double centerX, double centerY, double viewWidth) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.viewWidth = viewWidth;
        }
    }

    private RandomMandelbrotExplorer() {
    }

    static Result explore(Random random, double aspectRatio, CancelCheck cancel) {
        if (random == null || !(aspectRatio > 0)) {
            return null;
        }
        int passes = MIN_PASSES + random.nextInt(MAX_PASSES - MIN_PASSES + 1);
        double centerX = INITIAL_CENTER_X;
        double centerY = 0.0;
        double viewWidth = INITIAL_VIEW_WIDTH;
        double viewHeight = viewWidth * aspectRatio;

        for (int pass = 0; pass < passes; pass++) {
            if (isCancelled(cancel)) {
                return null;
            }
            int iterations = searchIterations(INITIAL_VIEW_WIDTH / viewWidth);
            List<Border> borders = findBorders(
                    centerX, centerY, viewWidth, viewHeight, iterations, cancel);
            if (borders == null || borders.isEmpty()) {
                return null;
            }
            Border border = chooseWeighted(borders, random);
            double factor = MIN_ZOOM + random.nextDouble() * (MAX_ZOOM - MIN_ZOOM);
            centerX += border.realOffset;
            centerY += border.imaginaryOffset;
            viewWidth /= factor;
            viewHeight /= factor;
        }
        return new Result(centerX, centerY, viewWidth);
    }

    private static List<Border> findBorders(
            double centerX,
            double centerY,
            double viewWidth,
            double viewHeight,
            int iterations,
            CancelCheck cancel) {
        int sampleWidth = SAMPLE_SIZE;
        int sampleHeight = Math.max(3, (int) Math.round(SAMPLE_SIZE * viewHeight / viewWidth));
        boolean[] interior = new boolean[sampleWidth * sampleHeight];
        FractalOperator operator = new OptimizedMandelbrotOperator();
        FractalOperator.EscapeSample sample = new FractalOperator.EscapeSample();
        Complex coordinate = new Complex();

        for (int y = 0; y < sampleHeight; y++) {
            if (isCancelled(cancel)) {
                return null;
            }
            double imaginary = centerY + (0.5 - y / (double) (sampleHeight - 1)) * viewHeight;
            for (int x = 0; x < sampleWidth; x++) {
                double real = centerX + (x / (double) (sampleWidth - 1) - 0.5) * viewWidth;
                coordinate.set(real, imaginary);
                interior[y * sampleWidth + x] = !operator.sampleInto(
                        coordinate, iterations, false, sample).escaped;
            }
        }

        List<Border> borders = new ArrayList<>();
        for (int y = 1; y < sampleHeight - 1; y++) {
            for (int x = 1; x < sampleWidth - 1; x++) {
                boolean current = interior[y * sampleWidth + x];
                int complexity = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if ((dx != 0 || dy != 0)
                                && interior[(y + dy) * sampleWidth + x + dx] != current) {
                            complexity++;
                        }
                    }
                }
                if (complexity > 0) {
                    borders.add(new Border(
                            (x / (double) (sampleWidth - 1) - 0.5) * viewWidth,
                            (0.5 - y / (double) (sampleHeight - 1)) * viewHeight,
                            complexity));
                }
            }
        }
        return borders;
    }

    private static Border chooseWeighted(List<Border> borders, Random random) {
        int total = 0;
        for (Border border : borders) {
            total += border.complexity;
        }
        int pick = random.nextInt(total);
        for (Border border : borders) {
            pick -= border.complexity;
            if (pick < 0) {
                return border;
            }
        }
        return borders.get(borders.size() - 1);
    }

    private static int searchIterations(double zoom) {
        double value = 40.0 * Math.pow(1.2, Math.log(zoom) / Math.log(2.0));
        return Math.max(10, Math.min(IterationSettings.MAX_ITER_CAP, (int) Math.round(value)));
    }

    private static boolean isCancelled(CancelCheck cancel) {
        return cancel != null && cancel.isCancelled();
    }

    private static final class Border {
        final double realOffset;
        final double imaginaryOffset;
        final int complexity;

        Border(double realOffset, double imaginaryOffset, int complexity) {
            this.realOffset = realOffset;
            this.imaginaryOffset = imaginaryOffset;
            this.complexity = complexity;
        }
    }
}
