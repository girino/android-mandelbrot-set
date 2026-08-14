package org.girino.frac.operators;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ComplexTest {
    @Test
    public void multiplicationUsesComplexArithmetic() {
        Complex result = Complex.times(new Complex(1, 2), new Complex(3, 4));

        assertEquals(-5.0, result.getReal(), 0.0);
        assertEquals(10.0, result.getImag(), 0.0);
    }

    @Test
    public void modulusMatchesPythagoreanDistance() {
        assertEquals(5.0, new Complex(3, 4).modulus(), 0.0);
    }
}
