package org.girino.frac.operators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** sampleContinue must match a full sample for all UI operators. */
class FractalOperatorContinueTest {

    static Stream<FractalOperator> uiOperators() {
        return Stream.of(
                new OptimizedMandelbrotOperator(),
                new BurningShipOperator(),
                new MandelbarOperator(),
                new CubeMandelbrotOperator(),
                new FourthMandelbrotOperator(),
                new ShipBarOperator(),
                new NovaOperator());
    }

    @ParameterizedTest
    @MethodSource("uiOperators")
    void sampleContinue_matchesFullSample(FractalOperator operator) {
        Complex point = new Complex(0, 0);
        int maxIter = 120;
        FractalOperator.EscapeSample full = operator.sample(point, maxIter, false);
        assertFalse(full.escaped, operator.getClass().getSimpleName());

        Complex scratch = new Complex();
        operator.readOrbitZ(scratch);
        FractalOperator.EscapeSample continued = operator.sampleContinue(
                point, full.iterations, scratch.getReal(), scratch.getImag(), maxIter, false);

        assertEquals(full.escaped, continued.escaped);
        assertEquals(full.iterations, continued.iterations);
        assertEquals(full.value, continued.value, 1e-12);
    }

    @ParameterizedTest
    @MethodSource("uiOperators")
    void sampleContinue_fromMidpoint_matchesFullSample(FractalOperator operator) {
        Complex point = new Complex(0, 0);
        int maxIter = 80;
        int mid = maxIter / 2;
        FractalOperator.EscapeSample partial = operator.sample(point, mid, false);
        assertFalse(partial.escaped, operator.getClass().getSimpleName());

        Complex scratch = new Complex();
        operator.readOrbitZ(scratch);
        FractalOperator.EscapeSample continued = operator.sampleContinue(
                point, partial.iterations, scratch.getReal(), scratch.getImag(), maxIter, false);
        FractalOperator.EscapeSample full = operator.sample(point, maxIter, false);

        assertEquals(full.escaped, continued.escaped);
        assertEquals(full.iterations, continued.iterations);
        assertEquals(full.value, continued.value, 1e-12);
    }
}
