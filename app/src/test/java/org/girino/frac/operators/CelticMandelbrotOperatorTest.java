package org.girino.frac.operators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class CelticMandelbrotOperatorTest {
    @Test
    void originStaysInterior() {
        CelticMandelbrotOperator celtic = new CelticMandelbrotOperator();
        assertEquals(1.0, celtic.apply(new Complex(0, 0), 40, false), 1e-12);
    }

    @Test
    void foldsRealAxisUnlikeMandelbrot() {
        CelticMandelbrotOperator celtic = new CelticMandelbrotOperator();
        OptimizedMandelbrotOperator mandelbrot = new OptimizedMandelbrotOperator();
        Complex c = new Complex(-0.75, 0.1);
        assertNotEquals(
                celtic.apply(c, 80, false),
                mandelbrot.apply(c, 80, false));
    }
}
