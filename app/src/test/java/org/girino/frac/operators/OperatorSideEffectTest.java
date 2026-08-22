package org.girino.frac.operators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class OperatorSideEffectTest {

    @Test
    void burningShipConjugatesInputPoint() {
        BurningShipOperator operator = new BurningShipOperator();
        Complex point = new Complex(0.3, 0.4);
        operator.apply(point, 20, false);

        assertEquals(0.3, point.getReal());
        assertEquals(-0.4, point.getImag());
    }

    @Test
    void shipBarConjugatesInputPoint() {
        ShipBarOperator operator = new ShipBarOperator();
        Complex point = new Complex(0.3, 0.4);
        operator.apply(point, 20, false);

        assertEquals(0.3, point.getReal());
        assertEquals(-0.4, point.getImag());
    }

    @Test
    void optimizedMandelbrotDoesNotMutateInputPoint() {
        OptimizedMandelbrotOperator operator = new OptimizedMandelbrotOperator();
        Complex point = new Complex(0.3, 0.4);
        operator.apply(point, 20, false);

        assertEquals(0.3, point.getReal());
        assertEquals(0.4, point.getImag());
        assertNotEquals(point.getImag(), -point.getImag());
    }
}
