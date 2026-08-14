package org.girino.frac.operators;


/**
 * operator for mandelbrot sets.. 
 * 
 * much simple than buddhabrot, huh?
 * @author girino
 *
 */
public class TestOperator extends FractalOperator {

	protected void beforeIteration(int step, Complex Z, Complex C, int maxiter) {
	    Z.set(-0.5,0);
	}
	protected void step(int step, Complex Z, Complex C, int maxiter) {
		Z.ladd(Complex.add(Complex.square(Z),Z),C);
	}
}
