package org.girino.frac.operators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JuliaOperatorTest {
    private final JuliaOperator operator = new JuliaOperator();

    @Test
    void seedNearFixedPointStaysInside() {
        // Z0 = C; with this Julia seed the origin is typically interior for modest maxiter.
        assertEquals(1.0, operator.apply(new Complex(0, 0), 40, false));
    }

    @Test
    void distantSeedEscapes() {
        double result = operator.apply(new Complex(2, 2), 40, false);
        assertTrue(result >= 0.0);
        assertTrue(result < 1.0);
    }
}
