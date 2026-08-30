package org.girino.frac.operators;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JuliaOperatorTest {
    @Test
    void differentPixelsSampleIndependently() {
        JuliaOperator julia = new JuliaOperator(0.285, 0.013);
        FractalOperator.EscapeSample interior =
                julia.sample(new Complex(0.3, 0.4), 80, false);
        FractalOperator.EscapeSample escaped =
                julia.sample(new Complex(5.0, 0.0), 80, false);
        assertTrue(interior.iterations == 80 && !interior.escaped);
        assertTrue(escaped.escaped && escaped.iterations < 80);
        assertNotEquals(interior.value, escaped.value);
    }

    @Test
    void differentSeedsChangeImage() {
        JuliaOperator a = new JuliaOperator(0.285, 0.013);
        JuliaOperator b = new JuliaOperator(-0.8, 0.156);
        Complex z0 = new Complex(0.3, 0.4);
        assertNotEquals(a.apply(z0, 80, false), b.apply(z0, 80, false));
    }
}
