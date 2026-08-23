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

    private static final FractalOperator[] OPERATORS = {
            new OptimizedMandelbrotOperator(),
            new BurningShipOperator(),
            new NovaOperator(),
            new MandelbarOperator(),
            new CubeMandelbrotOperator(),
            new FourthMandelbrotOperator(),
            new ShipBarOperator(),
    };

    private FormulaCatalog() {
    }

    public static String[] labels() {
        return LABELS.clone();
    }

    public static int size() {
        return OPERATORS.length;
    }

    public static FractalOperator get(int index) {
        return OPERATORS[index];
    }

    /** Index of the first catalog entry with the same class, or -1. */
    public static int indexOf(FractalOperator operator) {
        if (operator == null) {
            return -1;
        }
        Class<?> type = operator.getClass();
        for (int i = 0; i < OPERATORS.length; i++) {
            if (OPERATORS[i].getClass() == type) {
                return i;
            }
        }
        return -1;
    }
}
