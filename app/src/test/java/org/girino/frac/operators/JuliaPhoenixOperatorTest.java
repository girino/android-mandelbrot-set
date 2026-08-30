package org.girino.frac.operators;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JuliaPhoenixOperatorTest {
    @Test
    void differentPixelsSampleIndependently() {
        JuliaPhoenixOperator operator = new JuliaPhoenixOperator(0.285, 0.013, -0.5, 0.0);
        FractalOperator.EscapeSample interior =
                operator.sample(new Complex(0.3, 0.4), 80, false);
        FractalOperator.EscapeSample escaped =
                operator.sample(new Complex(5.0, 0.0), 80, false);
        assertTrue(interior.iterations == 80 && !interior.escaped);
        assertTrue(escaped.escaped && escaped.iterations < 80);
        assertNotEquals(interior.value, escaped.value);
    }

    @Test
    void differentPChangesImage() {
        JuliaPhoenixOperator a = new JuliaPhoenixOperator(0.285, 0.013, -0.5, 0.0);
        JuliaPhoenixOperator b = new JuliaPhoenixOperator(0.285, 0.013, 0.5, 0.0);
        Complex z0 = new Complex(0.3, 0.4);
        assertNotEquals(a.apply(z0, 80, false), b.apply(z0, 80, false));
    }

    @Test
    void differentCChangesImage() {
        JuliaPhoenixOperator a = new JuliaPhoenixOperator(0.285, 0.013, -0.5, 0.0);
        JuliaPhoenixOperator b = new JuliaPhoenixOperator(-0.8, 0.156, -0.5, 0.0);
        Complex z0 = new Complex(0.1, 0.55);
        FractalOperator.EscapeSample sampleA = a.sample(z0, 120, false);
        FractalOperator.EscapeSample sampleB = b.sample(z0, 120, false);
        assertTrue(
                sampleA.escaped != sampleB.escaped
                        || sampleA.iterations != sampleB.iterations
                        || Math.abs(sampleA.value - sampleB.value) > 1e-12);
    }
}
