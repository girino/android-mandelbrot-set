package org.girino.frac.operators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PhoenixOperatorTest {
    private final PhoenixOperator operator = new PhoenixOperator();

    @Test
    void originStaysInteriorForModestMaxIter() {
        assertEquals(1.0, operator.apply(new Complex(0, 0), 40, false));
    }

    @Test
    void distinctPCopiesChangeSample() {
        PhoenixOperator a = new PhoenixOperator(-0.5, 0.0);
        PhoenixOperator b = new PhoenixOperator(0.5, 0.0);
        Complex c = new Complex(0.3, 0.4);
        assertNotEquals(a.apply(c, 80, false), b.apply(c, 80, false));
    }

    @Test
    void orbitCheckpointUsesPrev() {
        assertTrue(operator.orbitCheckpointUsesPrev());
    }
}
