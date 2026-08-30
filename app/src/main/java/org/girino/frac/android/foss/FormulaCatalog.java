package org.girino.frac.android.foss;

import org.girino.frac.operators.BurningShipOperator;
import org.girino.frac.operators.CelticMandelbrotOperator;
import org.girino.frac.operators.CubeMandelbrotOperator;
import org.girino.frac.operators.FourthMandelbrotOperator;
import org.girino.frac.operators.FractalOperator;
import org.girino.frac.operators.JuliaOperator;
import org.girino.frac.operators.JuliaPhoenixOperator;
import org.girino.frac.operators.MandelbarOperator;
import org.girino.frac.operators.NovaOperator;
import org.girino.frac.operators.OptimizedMandelbrotOperator;
import org.girino.frac.operators.PhoenixOperator;
import org.girino.frac.operators.PerpendicularMandelbrotOperator;
import org.girino.frac.operators.ShipBarOperator;

/** Named fractal formulas available in the picker (issues #10 / #13). */
public final class FormulaCatalog {

    public static final int PHOENIX_INDEX = 7;
    public static final int JULIA_INDEX = 8;
    public static final int JULIA_PHOENIX_INDEX = 9;
    public static final int CELTIC_INDEX = 10;
    public static final int PERPENDICULAR_INDEX = 11;
    /** Tricorn / Mandelbar: conj(z)^2 + c (same operator, experiment label). */
    public static final int TRICORN_INDEX = 3;

    private static final String[] LABELS = {
            "Mandelbrot Set",
            "Burning Ship",
            "Nova Set",
            "Tricorn",
            "Cube Mandelbrot",
            "Mandelbrot to the fourth power",
            "Shipbar",
            "Phoenix",
            "Julia",
            "Julia Phoenix",
            "Celtic Mandelbrot",
            "Perpendicular Mandelbrot",
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
            case PHOENIX_INDEX:
                return createPhoenix(
                        PhoenixParamsStore.DEFAULT_P_RE, PhoenixParamsStore.DEFAULT_P_IM);
            case JULIA_INDEX:
                return createJulia(
                        JuliaParamsStore.DEFAULT_C_RE, JuliaParamsStore.DEFAULT_C_IM);
            case JULIA_PHOENIX_INDEX:
                return createJuliaPhoenix(
                        JuliaParamsStore.DEFAULT_C_RE,
                        JuliaParamsStore.DEFAULT_C_IM,
                        PhoenixParamsStore.DEFAULT_P_RE,
                        PhoenixParamsStore.DEFAULT_P_IM);
            case CELTIC_INDEX:
                return new CelticMandelbrotOperator();
            case PERPENDICULAR_INDEX:
                return new PerpendicularMandelbrotOperator();
            default:
                throw new IndexOutOfBoundsException("formula index " + index);
        }
    }

    public static FractalOperator createPhoenix(double pRe, double pIm) {
        return new PhoenixOperator(pRe, pIm);
    }

    public static FractalOperator createJulia(double cRe, double cIm) {
        return new JuliaOperator(cRe, cIm);
    }

    public static FractalOperator createJuliaPhoenix(double cRe, double cIm, double pRe, double pIm) {
        return new JuliaPhoenixOperator(cRe, cIm, pRe, pIm);
    }

    /** New instance of the same catalog formula as operator, or Mandelbrot. */
    public static FractalOperator createLike(FractalOperator operator) {
        if (operator instanceof PhoenixOperator) {
            PhoenixOperator phoenix = (PhoenixOperator) operator;
            return createPhoenix(phoenix.getPRe(), phoenix.getPIm());
        }
        if (operator instanceof JuliaOperator) {
            JuliaOperator julia = (JuliaOperator) operator;
            return createJulia(julia.getCRe(), julia.getCIm());
        }
        if (operator instanceof JuliaPhoenixOperator) {
            JuliaPhoenixOperator juliaPhoenix = (JuliaPhoenixOperator) operator;
            return createJuliaPhoenix(
                    juliaPhoenix.getCRe(),
                    juliaPhoenix.getCIm(),
                    juliaPhoenix.getPRe(),
                    juliaPhoenix.getPIm());
        }
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
        if (type == PhoenixOperator.class) {
            return PHOENIX_INDEX;
        }
        if (type == JuliaOperator.class) {
            return JULIA_INDEX;
        }
        if (type == JuliaPhoenixOperator.class) {
            return JULIA_PHOENIX_INDEX;
        }
        if (type == CelticMandelbrotOperator.class) {
            return CELTIC_INDEX;
        }
        if (type == PerpendicularMandelbrotOperator.class) {
            return PERPENDICULAR_INDEX;
        }
        return -1;
    }
}
