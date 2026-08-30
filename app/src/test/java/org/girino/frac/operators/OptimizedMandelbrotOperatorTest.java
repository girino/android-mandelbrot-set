package org.girino.frac.operators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OptimizedMandelbrotOperatorTest {
    private final OptimizedMandelbrotOperator operator = new OptimizedMandelbrotOperator();

    @Test
    void originRemainsInsideTheSet() {
        assertEquals(1.0, operator.apply(new Complex(0, 0), 40, false));
    }

    @Test
    void distantPointEscapesQuickly() {
        double result = operator.apply(new Complex(2, 0), 40, false);

        assertTrue(result > 0.0);
        assertTrue(result < 0.1);
    }

    @Test
    void smoothResultStaysInPaletteRange() {
        double result = operator.apply(new Complex(2, 0), 40, true);

        assertTrue(Double.isFinite(result));
        assertTrue(result >= 0.0);
        assertTrue(result < 1.0);
    }

    @Test
    void smoothInteriorPointMatchesDiscreteInterior() {
        assertEquals(1.0, operator.apply(new Complex(0, 0), 40, true));
    }

    @ParameterizedTest
    @CsvSource({
            "0.0, 0.0",
            "2.0, 0.0",
            "-0.75, 0.1",
            "0.25, 0.5",
            "-0.1, 0.65"
    })
    void matchesReferenceMandelbrotOperator(double real, double imag) {
        MandelbrotOperator reference = new MandelbrotOperator();
        Complex point = new Complex(real, imag);

        assertEquals(
                reference.apply(point, 40, false),
                operator.apply(new Complex(real, imag), 40, false),
                1e-12);
        assertEquals(
                reference.apply(point, 40, true),
                operator.apply(new Complex(real, imag), 40, true),
                1e-9);
    }
}
