package org.girino.frac.operators;


/**
 * operator for mandelbrot sets.. 
 * 
 * much simple than buddhabrot, huh?
 * @author girino
 *
 */
public class MandelbarOperator extends FractalOperator {

	protected void step(int step, Complex Z, Complex C, int maxiter) {
		Z.ladd(Complex.square(Complex.conjugate(Z)), C);
	}
}
