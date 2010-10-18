package org.girino.frac.operators;


/**
 * operator for mandelbrot sets.. 
 * 
 * much simple than buddhabrot, huh?
 * @author girino
 *
 */
public class BurningShipOperator implements FractalOperator {
	
	private static final double omega = 200;

	/**
	 * dunnoh how it is in the complex plane... must be someting.
	 * I guess it's Zn+1 = Zn*^Zn - C. Z0 = 0; 
	 */
	public int apply(double x, double y, int maxiter) {
		double xn = 0;
		double yn = 0;
		
		int j;
		
	    for(j = 0; (j < maxiter) && (xn*xn + yn*yn <= omega);j++) {
	    	double xnplus1 = xn*xn - yn*yn - x;
	    	yn = 2 * Math.abs(xn*yn) - y;
	    	xn = xnplus1;
	    }
	    return j;
	}

}
