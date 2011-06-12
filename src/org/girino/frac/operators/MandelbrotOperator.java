package org.girino.frac.operators;


/**
 * operator for mandelbrot sets.. 
 * 
 * much simple than buddhabrot, huh?
 * @author girino
 *
 */
public class MandelbrotOperator extends FractalOperator {

	protected void step(int step, Complex Z, Complex C, int maxiter) {
		Z.lsquare(Z);
		Z.ladd(Z, C);
	}
}
