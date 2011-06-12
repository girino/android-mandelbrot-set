package org.girino.frac.operators;


/**
 * operator for mandelbrot sets.. 
 * 
 * much simple than buddhabrot, huh?
 * @author girino
 *
 */
public class TesteMandelbrotOperator extends FractalOperator {
	
	/**
	 * yeah,m teh old stuff, Z = Z^3 + C.
	 */
	protected void step(int step, Complex Z, Complex C, int maxiter) {
		Z.ladd(Complex.square(Complex.conjugate(Z)), C);
	}
}
