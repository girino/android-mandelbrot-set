package org.girino.frac.operators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class PerpendicularMandelbrotOperatorTest {
    @Test
    void originStaysInterior() {
        PerpendicularMandelbrotOperator perpendicular = new PerpendicularMandelbrotOperator();
        assertEquals(1.0, perpendicular.apply(new Complex(0, 0), 40, false), 1e-12);
    }

    @Test
    void imaginaryUpdateUsesAbsXWhenRealPartPositive() {
        PerpendicularMandelbrotOperator perpendicular = new PerpendicularMandelbrotOperator();
        OptimizedMandelbrotOperator mandelbrot = new OptimizedMandelbrotOperator();
        Complex c = new Complex(0.4, 0.2);
        perpendicular.sample(c, 2, false);
        mandelbrot.sample(c, 2, false);
        Complex pz = new Complex();
        Complex mz = new Complex();
        perpendicular.readOrbitZ(pz);
        mandelbrot.readOrbitZ(mz);
        assertEquals(0.52, pz.getReal(), 1e-12);
        assertEquals(0.52, mz.getReal(), 1e-12);
        assertNotEquals(pz.getImag(), mz.getImag(), 1e-12);
        assertEquals(0.04, pz.getImag(), 1e-12);
        assertEquals(0.36, mz.getImag(), 1e-12);
    }
}
