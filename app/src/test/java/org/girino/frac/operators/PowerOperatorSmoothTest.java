package org.girino.frac.operators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Smooth renormalization log(k) for z^k + c (issue #40). */
class PowerOperatorSmoothTest {

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5})
    void iterationPower_matchesMapDegree(int power) throws Exception {
        FractalOperator operator = operatorForPower(power);
        assertEquals(Math.log(power), logEscapeRadius(operator), 1e-12);
    }

    @ParameterizedTest
    @CsvSource({
            "1.5, 0.8, 80, 3",
            "1.2, 0.9, 80, 4"
    })
    void smoothSample_usesLogK(double re, double im, int maxIter, int power) {
        FractalOperator operator = operatorForPower(power);
        Complex point = new Complex(re, im);
        FractalOperator.EscapeSample probe = operator.sample(point, maxIter, false);
        assertTrue(probe.escaped, "test point must escape");

        Complex z = new Complex();
        operator.readOrbitZ(z);
        double expected = smoothMu(probe.iterations, z, maxIter, Math.log(power));
        double actual = operator.apply(point, maxIter, true);

        assertEquals(expected, actual, 1e-9);
        assertNotEquals(
                smoothMu(probe.iterations, z, maxIter, Math.log(2.0)),
                actual,
                1e-6,
                "log(2) renormalization must differ for power " + power);
    }

    private static FractalOperator operatorForPower(int power) {
        switch (power) {
            case 3:
                return new CubeMandelbrotOperator();
            case 4:
                return new FourthMandelbrotOperator();
            case 5:
                return new FifthMandelbrotOperator();
            default:
                throw new IllegalArgumentException("power " + power);
        }
    }

    private static double logEscapeRadius(FractalOperator operator) throws Exception {
        var method = FractalOperator.class.getDeclaredMethod("getLogEscapeRadius");
        method.setAccessible(true);
        return (double) method.invoke(operator);
    }

    private static double smoothMu(int step, Complex z, int maxIter, double logK) {
        if (step >= maxIter) {
            return 0.0;
        }
        double mu = step - (Math.log(Math.log(Complex.modulus(z)))) / logK;
        mu /= maxIter;
        if (mu >= 1.0) {
            mu = 0;
        }
        if (mu < 0) {
            mu = 0;
        }
        return mu;
    }
}
