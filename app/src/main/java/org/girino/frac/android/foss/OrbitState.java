package org.girino.frac.android.foss;

import org.girino.frac.operators.Complex;
import org.girino.frac.operators.FractalOperator;

/**
 * Per-pixel orbit checkpoints for Adaptive warm-start (continue from pass-1
 * or the previous border round instead of restarting from iteration 0).
 */
public final class OrbitState {

    public final int[] iter;
    public final double[] re;
    public final double[] im;

    public OrbitState(int pixelCount) {
        iter = new int[pixelCount];
        re = new double[pixelCount];
        im = new double[pixelCount];
    }

    /** True when a non-zero checkpoint exists for index. */
    public boolean hasCheckpoint(int index) {
        return iter[index] > 0;
    }

    public void storeInterior(int index, FractalOperator operator, FractalOperator.EscapeSample sample, Complex scratch) {
        if (sample.escaped) {
            iter[index] = 0;
            return;
        }
        iter[index] = sample.iterations;
        operator.readOrbitZ(scratch);
        re[index] = scratch.getReal();
        im[index] = scratch.getImag();
    }

    public void clear(int index) {
        iter[index] = 0;
    }
}
