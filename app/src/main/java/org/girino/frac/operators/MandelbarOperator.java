package org.girino.frac.operators;


/**
 * Tricorn (Mandelbar): z(n+1) = conjugate(z(n))^2 + c.
 * Scalar step — no Complex allocation per iteration (issue #36).
 */
public class MandelbarOperator extends FractalOperator {

	protected void step(int step, Complex Z, Complex C, int maxiter) {
		double a = Z.getReal();
		double b = Z.getImag();
		double re = a * a - b * b + C.getReal();
		double im = -2.0 * a * b + C.getImag();
		Z.set(re, im);
	}
}
