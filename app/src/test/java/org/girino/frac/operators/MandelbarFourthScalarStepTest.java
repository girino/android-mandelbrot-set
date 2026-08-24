package org.girino.frac.operators;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Scalar Mandelbar / Fourth steps must match the old Complex-based step (issue #36). */
class MandelbarFourthScalarStepTest {

    private static FractalOperator legacyMandelbar() {
        return new FractalOperator() {
            @Override
            protected void step(int step, Complex Z, Complex C, int maxiter) {
                Z.ladd(Complex.square(Complex.conjugate(Z)), C);
            }
        };
    }

    private static FractalOperator legacyFourth() {
        return new FractalOperator() {
            @Override
            protected void step(int step, Complex Z, Complex C, int maxiter) {
                Z.ladd(Complex.square(Complex.square(Z)), C);
            }
        };
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 40, false",
            "0.3, 0.4, 60, false",
            "-0.75, 0.1, 80, false",
            "0.1, 0.5, 40, false",
            "0.1, -0.5, 40, false",
            "0.3, 0.4, 60, true"
    })
    void mandelbarMatchesLegacyStep(double re, double im, int maxIter, boolean smooth) {
        assertSampleMatches(legacyMandelbar(), new MandelbarOperator(), re, im, maxIter, smooth);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 40, false",
            "0.3, 0.4, 60, false",
            "-0.75, 0.1, 80, false",
            "2, 0, 40, false",
            "0.3, 0.4, 60, true"
    })
    void fourthMatchesLegacyStep(double re, double im, int maxIter, boolean smooth) {
        assertSampleMatches(legacyFourth(), new FourthMandelbrotOperator(), re, im, maxIter, smooth);
    }

    private static void assertSampleMatches(
            FractalOperator expected,
            FractalOperator actual,
            double re,
            double im,
            int maxIter,
            boolean smooth) {
        Complex point = new Complex(re, im);
        FractalOperator.EscapeSample want = expected.sample(point, maxIter, smooth);
        FractalOperator.EscapeSample got = actual.sample(point, maxIter, smooth);
        assertEquals(want.escaped, got.escaped);
        assertEquals(want.iterations, got.iterations);
        assertEquals(want.value, got.value, 1e-12);
    }
}
