package org.girino.frac.operators;


/**
 * operator for mandelbrot sets.. 
 * 
 * much simple than buddhabrot, huh?
 * @author girino
 *
 */
public class TestOperator implements FractalOperator {
	
	private static final double omega = 4.0;

	public int apply(double x, double y, int maxiter) {
		/*
		 z->z^2+z+c, the 
		 critical point is given by 2z+1=0, so start with z=-1/2
		 */
	    Complex C = new Complex(x,y);
	    Complex Z = new Complex(-0.5,0);
	    int j = 0;
	    
	    for(j = 0; (j < maxiter) && (Complex.modulus(Z) <= omega);j++) 
	      Z = Complex.add(Complex.add(Complex.square(Z),Z),C);
	    
	    return j;
	}

}
