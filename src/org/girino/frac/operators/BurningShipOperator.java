package org.girino.frac.operators;


/**
 * operator for mandelbrot sets.. 
 * 
 * much simple than buddhabrot, huh?
 * @author girino
 *
 */
public class BurningShipOperator extends FractalOperator {
	
	/**
	 * dunnoh how it is in the complex plane... must be someting.
	 * I guess it's Zn+1 = Zn*^Zn - C. Z0 = 0; 
	 */
	Complex tmp = new Complex();
	protected void beforeIteration(int step, Complex Z, Complex C, int maxiter) {
		C.lconjugate(C);
	}

	protected void step(int step, Complex Z, Complex C, int maxiter) {
    	tmp.ltimes(Z, Z);
    	if (tmp.getImag() < 0) {
    		tmp.lconjugate(tmp);
    	}
    	Z.lsub(tmp, C);
	}

}
