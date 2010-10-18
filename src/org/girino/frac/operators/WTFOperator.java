package org.girino.frac.operators;


/**
 * this is a cool fractal function i found while playng 
 * around with mandelbrot sets. kept it here for fun. 
 * @author girino
 *
 */
public class WTFOperator implements FractalOperator {
	
	private static final double omega = 4.0;

	/**
	 * the mistake is I add the previous point, not the original one.
	 */
	public int apply(double x, double y, int maxiter) {
	    int v = 0;
	    for(; v < maxiter && (x * x + y * y) < omega; v++) {
	    	double t = x + x * x - y * y;
	    	y = y + x * y * 2f;
	    	x = t;
	  	}
	    return v;
	}


}
