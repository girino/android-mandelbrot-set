package org.girino.frac.operators;


/**
 * operator for mandelbrot sets.. 
 * 
 * much simple than buddhabrot, huh?
 * @author girino
 *
 */
public class MandelbrotOperator implements FractalOperator {
	
	private static final double omega = 4.0;

	/**
	 * yeah,m teh old stuff, Z = Z^2 + C.
	 */
	public int apply(double x, double y, int maxiter) {
	    Complex C = new Complex(x,y);
	    Complex Z = new Complex(0,0);
	    int j = 0;
	    
	    for(j = 0; (j < maxiter) && (Complex.modulus(Z) <= omega);j++) 
	      Z = Complex.add(Complex.square(Z),C);
	    
	    return j;
	}


}