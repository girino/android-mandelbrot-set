package org.girino.frac.operators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ComplexTest {

    @Test
    void multiplicationUsesComplexArithmetic() {
        Complex result = Complex.times(new Complex(1, 2), new Complex(3, 4));

        assertEquals(-5.0, result.getReal());
        assertEquals(10.0, result.getImag());
    }

    @Test
    void additionSumsComponents() {
        Complex result = Complex.add(new Complex(1.5, -2), new Complex(0.5, 4));

        assertEquals(2.0, result.getReal());
        assertEquals(2.0, result.getImag());
    }

    @Test
    void laddAndLsubMutateDestination() {
        Complex dest = new Complex();
        dest.ladd(new Complex(3, 1), new Complex(2, 4));
        assertEquals(5.0, dest.getReal());
        assertEquals(5.0, dest.getImag());

        dest.lsub(new Complex(3, 1), new Complex(2, 4));
        assertEquals(1.0, dest.getReal());
        assertEquals(-3.0, dest.getImag());
    }

    @Test
    void ltimesMatchesStaticTimes() {
        Complex a = new Complex(1, 2);
        Complex b = new Complex(3, 4);
        Complex dest = new Complex();
        dest.ltimes(a, b);
        Complex expected = Complex.times(a, b);

        assertEquals(expected.getReal(), dest.getReal());
        assertEquals(expected.getImag(), dest.getImag());
    }

    @Test
    void squareIsSelfProduct() {
        Complex value = new Complex(2, 3);
        Complex squared = Complex.square(value);
        Complex expected = Complex.times(value, value);

        assertEquals(expected.getReal(), squared.getReal());
        assertEquals(expected.getImag(), squared.getImag());
    }

    @Test
    void inPlaceSquareDoesNotCorruptAliasing() {
        Complex value = new Complex(2, 3);
        Complex expected = Complex.square(value);
        value.square();

        assertEquals(expected.getReal(), value.getReal());
        assertEquals(expected.getImag(), value.getImag());
    }

    @Test
    void modulusMatchesPythagoreanDistance() {
        assertEquals(5.0, new Complex(3, 4).modulus());
        assertEquals(25.0, new Complex(3, 4).modulus2());
        assertEquals(25.0, Complex.modulus2(new Complex(3, 4)));
    }

    @Test
    void conjugateFlipsImaginarySign() {
        Complex conjugated = Complex.conjugate(new Complex(2, -5));
        assertEquals(2.0, conjugated.getReal());
        assertEquals(5.0, conjugated.getImag());

        Complex value = new Complex(2, -5);
        value.conjugate();
        assertEquals(2.0, value.getReal());
        assertEquals(5.0, value.getImag());
    }

    @Test
    void minusNegatesBothParts() {
        Complex negated = Complex.minus(new Complex(1.5, -2.5));
        assertEquals(-1.5, negated.getReal());
        assertEquals(2.5, negated.getImag());
    }

    @Test
    void inverseIsMultiplicativeReciprocal() {
        Complex value = new Complex(1, 1);
        Complex inverse = Complex.inverse(value);
        Complex product = Complex.times(value, inverse);

        assertEquals(1.0, product.getReal(), 1e-12);
        assertEquals(0.0, product.getImag(), 1e-12);
    }

    @Test
    void absTakesComponentWiseAbsoluteValue() {
        Complex value = new Complex(-3, -4);
        value.abs();
        assertEquals(3.0, value.getReal());
        assertEquals(4.0, value.getImag());
    }

    @Test
    void setCopiesValuesAndToStringFormatsPair() {
        Complex value = new Complex();
        value.set(new Complex(7, -8));
        assertEquals(7.0, value.getReal());
        assertEquals(-8.0, value.getImag());
        assertEquals("{7.0, -8.0}", value.toString());
    }
}
