package org.girino.frac.operators;


/**
 * operator for mandelbrot sets.. 
 * 
 * much simple than buddhabrot, huh?
 * @author girino
 *
 */
public class CubeMandelbrotOperator extends FractalOperator {
	
    Complex tmp = new Complex();
	/**
	 * yeah,m teh old stuff, Z = Z^3 + C.
	 */
	protected void step(int step, Complex Z, Complex C, int maxiter) {
		tmp.ltimes(Z, Z);
		tmp.ltimes(tmp, Z);
		Z.ladd(tmp, C);
	}

}
