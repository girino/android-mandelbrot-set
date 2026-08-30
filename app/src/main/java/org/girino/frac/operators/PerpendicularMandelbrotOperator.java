package org.girino.frac.operators;

/**
 * Perpendicular Mandelbrot: z(n+1) = (|x(n)| - i*y(n))^2 + c.
 * Scalar form: x' = x^2 - y^2 + c_x, y' = -2|x|y + c_y.
 */
public class PerpendicularMandelbrotOperator extends FractalOperator {

    @Override
    protected void step(int step, Complex Z, Complex C, int maxiter) {
        double x = Z.getReal();
        double y = Z.getImag();
        double ax = Math.abs(x);
        double newRe = ax * ax - y * y + C.getReal();
        double newIm = -2.0 * ax * y + C.getImag();
        Z.set(newRe, newIm);
    }
}
