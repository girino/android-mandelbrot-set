package org.girino.frac.operators;


/**
 * operator for mandelbrot sets.. 
 * 
 * much simple than buddhabrot, huh?
 * @author girino
 *
 */
public class FifthMandelbrotOperator extends FractalOperator {
	
	/**
	 * yeah,m teh old stuff, Z = Z^5 + C.
	 */
	Complex tmp = new Complex();
	public void step(int step, Complex Z, Complex C, int maxiter) {
		tmp.lsquare(Z);
		tmp.square();
		tmp.ltimes(Z, tmp);
		Z.ladd(tmp,C);
	}
}
