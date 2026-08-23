package org.girino.frac.android.foss;

import org.girino.frac.operators.BurningShipOperator;
import org.girino.frac.operators.CubeMandelbrotOperator;
import org.girino.frac.operators.FourthMandelbrotOperator;
import org.girino.frac.operators.FractalOperator;
import org.girino.frac.operators.MandelbarOperator;
import org.girino.frac.operators.NovaOperator;
import org.girino.frac.operators.OptimizedMandelbrotOperator;
import org.girino.frac.operators.ShipBarOperator;

/** Named fractal formulas available in the picker (issues #10 / #13). */
public final class FormulaCatalog {
    private static final String[] LABELS = {
            "Mandelbrot Set",
            "Burning Ship",
            "Nova Set",
            "Mandelbar",
            "Cube Mandelbrot",
            "Mandelbrot to the fourth power",
            "Shipbar",
    };

    private FormulaCatalog() {
    }

    public static String[] labels() {
        return LABELS.clone();
    }

    public static int size() {
        return LABELS.length;
    }

    /** Shared instance for UI / single-thread use. Prefer create for workers. */
    public static FractalOperator get(int index) {
        return create(index);
    }

    /**
     * Fresh operator instance (issue #25). FractalOperator keeps mutable
     * iteration state, so parallel workers each need their own copy.
     */
    public static FractalOperator create(int index) {
        switch (index) {
            case 0:
                return new OptimizedMandelbrotOperator();
            case 1:
                return new BurningShipOperator();
            case 2:
                return new NovaOperator();
            case 3:
                return new MandelbarOperator();
            case 4:
                return new CubeMandelbrotOperator();
            case 5:
                return new FourthMandelbrotOperator();
            case 6:
                return new ShipBarOperator();
            default:
                throw new IndexOutOfBoundsException("formula index " + index);
        }
    }

    /** New instance of the same catalog formula as operator, or Mandelbrot. */
    public static FractalOperator createLike(FractalOperator operator) {
        int index = indexOf(operator);
        return create(index >= 0 ? index : 0);
    }

    /** Index of the first catalog entry with the same class, or -1. */
    public static int indexOf(FractalOperator operator) {
        if (operator == null) {
            return -1;
        }
        Class<?> type = operator.getClass();
        if (type == OptimizedMandelbrotOperator.class) {
            return 0;
        }
        if (type == BurningShipOperator.class) {
            return 1;
        }
        if (type == NovaOperator.class) {
            return 2;
        }
        if (type == MandelbarOperator.class) {
            return 3;
        }
        if (type == CubeMandelbrotOperator.class) {
            return 4;
        }
        if (type == FourthMandelbrotOperator.class) {
            return 5;
        }
        if (type == ShipBarOperator.class) {
            return 6;
        }
        return -1;
    }
}
