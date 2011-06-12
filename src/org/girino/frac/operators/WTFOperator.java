package org.girino.frac.operators;


/**
 * this is a cool fractal function i found while playng 
 * around with mandelbrot sets. kept it here for fun. 
 * @author girino
 *
 */
public class WTFOperator extends FractalOperator {
	
	/**
	 * the mistake is I add the previous point, not the original one.
	 */
    Complex tmp = new Complex();
    protected void step(int step, Complex Z, Complex C, int maxiter) {
    	tmp.lsquare(C);
    	C.ladd(tmp, C);
	}
    
    protected void afterIteration(int step, Complex Z, Complex C, int maxiter) {
    	Z.set(C);
    }
    
    protected boolean diverge(int step, Complex Z, Complex C, int maxiter) {
    	return C.modulus2() < omega2;
    }
}
