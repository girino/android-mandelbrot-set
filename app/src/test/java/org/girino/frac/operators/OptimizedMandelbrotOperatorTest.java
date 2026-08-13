package org.girino.frac.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OptimizedMandelbrotOperatorTest {
    private final OptimizedMandelbrotOperator operator = new OptimizedMandelbrotOperator();

    @Test
    public void originRemainsInsideTheSet() {
        assertEquals(1.0, operator.apply(new Complex(0, 0), 40, false), 0.0);
    }

    @Test
    public void distantPointEscapesQuickly() {
        double result = operator.apply(new Complex(2, 0), 40, false);

        assertTrue(result > 0.0);
        assertTrue(result < 0.1);
    }

    @Test
    public void smoothResultStaysInPaletteRange() {
        double result = operator.apply(new Complex(2, 0), 40, true);

        assertTrue(Double.isFinite(result));
        assertTrue(result >= 0.0);
        assertTrue(result < 1.0);
    }
}
