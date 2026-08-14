package org.girino.frac.operators;


/**
 * operator for mandelbrot sets.. 
 * 
 * much simple than buddhabrot, huh?
 * @author girino
 *
 */
public class FourthMandelbrotOperator extends FractalOperator {
	public void step(int step, Complex Z, Complex C, int maxiter) {
		Z.ladd(Complex.square(Complex.square(Z)),C);
	}
}
