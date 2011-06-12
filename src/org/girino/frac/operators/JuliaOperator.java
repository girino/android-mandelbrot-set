package org.girino.frac.operators;


/**
 * operator for mandelbrot sets.. 
 * 
 * much simple than buddhabrot, huh?
 * @author girino
 *
 */
public class JuliaOperator extends FractalOperator {
	
	private Complex fixedPoint = new Complex(0.285,0.013); 
	/**
	 * yeah,m teh old stuff, Z = Z^2 + C. but now I get to iterate over Z, no C.
	 * C is fixed.
	 */
	protected void beforeIteration(int step, Complex Z, Complex C, int maxiter) {
		Z.set(C);
	}

	public void step(int step, Complex Z, Complex C, int maxiter) {
		Z.square();
		Z.ladd(Z,fixedPoint);
	}

}
