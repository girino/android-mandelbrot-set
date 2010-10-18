package org.girino.frac.operators;


/**
 * operator for mandelbrot sets.. 
 * 
 * much simple than buddhabrot, huh?
 * @author girino
 *
 */
public class JuliaOperator implements FractalOperator {
	
	private static final double omega = 4.0;
	private Complex C = new Complex(0.285,0.013); 

	/**
	 * yeah,m teh old stuff, Z = Z^2 + C. but now I get to iterate over Z, no C.
	 * C is fixed.
	 */
	public int apply(double x, double y, int maxiter) {
	    Complex Z = new Complex(x,y);
	    int j = 0;
	    
	    for(j = 0; (j < maxiter) && (Complex.modulus(Z) <= omega);j++) 
	      Z = Complex.add(Complex.square(Z),C);
	    
	    return j;
	}

	public void config(Double cx, Double cy) {
		if (cx == null) {
			cx = 0.285;
		}
		if (cy == null) {
			cy = 0.0;
		}
		this.C = new Complex(cx, cy);
	}

}
