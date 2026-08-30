package org.girino.frac.operators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class UiOperatorsContractTest {

    static Stream<FractalOperator> classicUiOperators() {
        return Stream.of(
                new OptimizedMandelbrotOperator(),
                new BurningShipOperator(),
                new MandelbarOperator(),
                new CubeMandelbrotOperator(),
                new FourthMandelbrotOperator(),
                new ShipBarOperator());
    }

    static Stream<FractalOperator> allUiOperators() {
        return Stream.concat(classicUiOperators(), Stream.of(new NovaOperator()));
    }

    @ParameterizedTest
    @MethodSource("classicUiOperators")
    void originStaysInsideForDiscreteColoring(FractalOperator operator) {
        assertEquals(1.0, operator.apply(new Complex(0, 0), 40, false));
        assertEquals(1.0, operator.apply(new Complex(0, 0), 40, true));
    }

    @ParameterizedTest
    @MethodSource("allUiOperators")
    void farAwayPointEscapesWithFinitePaletteValue(FractalOperator operator) {
        double discrete = operator.apply(new Complex(10, 10), 40, false);
        double smooth = operator.apply(new Complex(10, 10), 40, true);

        assertTrue(discrete >= 0.0 && discrete <= 1.0, "discrete=" + discrete);
        assertTrue(discrete < 1.0, "expected escape, discrete=" + discrete);
        assertTrue(Double.isFinite(smooth));
        assertTrue(smooth >= 0.0 && smooth < 1.0, "smooth=" + smooth);
    }

    @Test
    void novaApplyAtOriginIsFinitePaletteValue() {
        double discrete = new NovaOperator().apply(new Complex(0, 0), 40, false);
        assertTrue(Double.isFinite(discrete));
        assertTrue(discrete >= 0.0 && discrete <= 1.0);
    }

    @Test
    void mandelbarIsNotMirrorSymmetricLikeMandelbrot() {
        MandelbarOperator mandelbar = new MandelbarOperator();
        OptimizedMandelbrotOperator mandelbrot = new OptimizedMandelbrotOperator();
        Complex upper = new Complex(0.1, 0.5);
        Complex lower = new Complex(0.1, -0.5);

        assertEquals(
                mandelbrot.apply(upper, 40, false),
                mandelbrot.apply(lower, 40, false),
                1e-12);
        assertTrue(
                Math.abs(mandelbar.apply(upper, 40, false) - mandelbar.apply(lower, 40, false))
                        > 1e-12
                        || Math.abs(
                                        mandelbar.apply(upper, 40, false)
                                                - mandelbrot.apply(upper, 40, false))
                                > 1e-12);
    }
}
