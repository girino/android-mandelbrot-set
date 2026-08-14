package org.girino.frac.operators;


/**
 * operator for mandelbrot sets.. 
 * 
 * much simple than buddhabrot, huh?
 * @author girino
 *
 */
public class ShipBarOperator extends FractalOperator {

	protected void beforeIteration(int step, Complex Z, Complex C, int maxiter) {
		C.conjugate();
	}
	protected void step(int step, Complex Z, Complex C, int maxiter) {
		Z.abs();
		Z.square();
		Z.lsub(Z, C);
	}
}
