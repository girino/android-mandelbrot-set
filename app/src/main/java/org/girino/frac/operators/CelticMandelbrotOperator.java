package org.girino.frac.operators;

/**
 * Celtic Mandelbrot: z(n+1) = |Re(z(n)^2)| + i*Im(z(n)^2) + c.
 */
public class CelticMandelbrotOperator extends FractalOperator {

    @Override
    protected void step(int step, Complex Z, Complex C, int maxiter) {
        double a = Z.getReal();
        double b = Z.getImag();
        double z2Re = a * a - b * b;
        double z2Im = 2.0 * a * b;
        double newRe = Math.abs(z2Re) + C.getReal();
        double newIm = z2Im + C.getImag();
        Z.set(newRe, newIm);
    }
}
