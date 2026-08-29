package org.girino.frac.android.foss;

import org.girino.frac.operators.Complex;
import org.girino.frac.operators.FractalOperator;

/**
 * Per-pixel orbit checkpoints for Adaptive warm-start (continue from pass-1
 * or the previous border round instead of restarting from iteration 0).
 *
 * Issue #39 experiment: re/im stored as float (iter stays int) to cut RAM and
 * bandwidth; sampleContinue still receives double at the operator boundary.
 */
public final class OrbitState {

    private final int[] iter;
    private final float[] re;
    private final float[] im;

    public OrbitState(int pixelCount) {
        iter = new int[pixelCount];
        re = new float[pixelCount];
        im = new float[pixelCount];
    }

    public int pixelCount() {
        return iter.length;
    }

    /** Approximate heap for this buffer (issue #39). */
    public long estimateHeapBytes() {
        return (long) iter.length * (4L + 4L + 4L);
    }

    /** Footprint of the pre-#39 double layout for the same pixel count. */
    public static long legacyDoubleLayoutHeapBytes(int pixelCount) {
        return (long) pixelCount * (4L + 8L + 8L);
    }

    /** True when a non-zero checkpoint exists for index. */
    public boolean hasCheckpoint(int index) {
        return iter[index] > 0;
    }

    public int checkpointIter(int index) {
        return iter[index];
    }

    public double checkpointRe(int index) {
        return re[index];
    }

    public double checkpointIm(int index) {
        return im[index];
    }

    public void storeInterior(
            int index,
            FractalOperator operator,
            FractalOperator.EscapeSample sample,
            Complex scratch) {
        if (sample.escaped) {
            iter[index] = 0;
            return;
        }
        iter[index] = sample.iterations;
        operator.readOrbitZ(scratch);
        re[index] = (float) scratch.getReal();
        im[index] = (float) scratch.getImag();
    }

    public void clear(int index) {
        iter[index] = 0;
    }
}
