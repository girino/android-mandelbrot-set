package org.girino.frac.operators;


/**
 * z_{n+1} = z_n^4 + c.
 * Scalar step — no Complex allocation per iteration (issue #36).
 */
public class FourthMandelbrotOperator extends FractalOperator {

	public void step(int step, Complex Z, Complex C, int maxiter) {
		double x = Z.getReal();
		double y = Z.getImag();
		double u = x * x - y * y;
		double v = 2.0 * x * y;
		double re = u * u - v * v + C.getReal();
		double im = 2.0 * u * v + C.getImag();
		Z.set(re, im);
	}
}
