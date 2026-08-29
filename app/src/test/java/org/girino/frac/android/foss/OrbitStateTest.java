package org.girino.frac.android.foss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.girino.frac.operators.Complex;
import org.girino.frac.operators.FractalOperator;
import org.girino.frac.operators.OptimizedMandelbrotOperator;
import org.junit.jupiter.api.Test;

/** Issue #39 — OrbitState float checkpoints and warm-start equivalence. */
class OrbitStateTest {

    @Test
    void floatLayoutUsesLessHeapThanLegacyDoubleLayout() {
        int pixels = 1920 * 1080;
        OrbitState orbit = new OrbitState(pixels);
        assertTrue(orbit.estimateHeapBytes() < OrbitState.legacyDoubleLayoutHeapBytes(pixels));
        assertEquals((long) pixels * 12L, orbit.estimateHeapBytes());
        assertEquals((long) pixels * 20L, OrbitState.legacyDoubleLayoutHeapBytes(pixels));
    }

    @Test
    void floatCheckpointContinueMatchesFullSampleOnInteriorGrid() {
        OptimizedMandelbrotOperator operator = new OptimizedMandelbrotOperator();
        int pass1 = 64;
        int cap = 256;
        OrbitState orbit = new OrbitState(1);
        Complex scratch = new Complex();

        for (double re = -0.8; re <= 0.2; re += 0.05) {
            for (double im = -0.6; im <= 0.6; im += 0.05) {
                Complex point = new Complex(re, im);
                FractalOperator.EscapeSample partial = operator.sample(point, pass1, false);
                if (partial.escaped) {
                    continue;
                }
                orbit.storeInterior(0, operator, partial, scratch);

                FractalOperator.EscapeSample fromCheckpoint = operator.sampleContinue(
                        new Complex(re, im),
                        orbit.checkpointIter(0),
                        orbit.checkpointRe(0),
                        orbit.checkpointIm(0),
                        cap,
                        false);
                FractalOperator.EscapeSample full = operator.sample(new Complex(re, im), cap, false);

                assertEquals(full.escaped, fromCheckpoint.escaped, "escaped @" + re + "," + im);
                assertEquals(full.iterations, fromCheckpoint.iterations, "iter @" + re + "," + im);
                assertEquals(full.value, fromCheckpoint.value, 1e-12, "value @" + re + "," + im);
            }
        }
    }

    @Test
    void clearRemovesCheckpoint() {
        OrbitState orbit = new OrbitState(4);
        OptimizedMandelbrotOperator operator = new OptimizedMandelbrotOperator();
        Complex scratch = new Complex();
        FractalOperator.EscapeSample sample = operator.sample(new Complex(0, 0), 40, false);
        assertFalse(sample.escaped);

        orbit.storeInterior(2, operator, sample, scratch);
        assertTrue(orbit.hasCheckpoint(2));

        orbit.clear(2);
        assertFalse(orbit.hasCheckpoint(2));
    }
}
