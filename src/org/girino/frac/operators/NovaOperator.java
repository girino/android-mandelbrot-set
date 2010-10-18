package org.girino.frac.operators;


/**
 * operator for mandelbrot sets.. 
 * 
 * much simple than buddhabrot, huh?
 * @author girino
 *
 */
public class NovaOperator implements FractalOperator {
	
	private static final double omega = 4.0;

	/**
	 *     Fractal Geometry: Nova Fractal
	 *     z(n) := z(n-1) - (2 * f(z(n-1))/f’(z(n-1))) + c. 
	 *     Where z ∈ C, c ∈ C and f(z)=z^3-1.
	 *
	 */
	public int apply(double x, double y, int maxiter) {
	    Complex C = new Complex(x,y);
	    Complex Z = new Complex(4,0);
	    int j = 0;
	    
	    for(j = 0; (j < maxiter) && (Complex.modulus(Z) <= omega);j++) {
	    	Complex tmp = Complex.times(f(Z), Complex.inverse(f_(Z)));
	    	//System.out.println(f(Z));
	    	Complex tmp2 = Complex.times(new Complex(2), tmp);
	    	//System.out.println(tmp2);
	    	Complex tmp3 = Complex.add(Complex.minus(tmp2), C);
	    	//System.out.println(tmp3);
	    	//System.out.println(Z.toString() + " - " + Complex.add(Z, tmp3).toString());
	    	Z = Complex.add(Z, tmp3);
	    }
	    
	    return j;
	}
	
	public Complex f(Complex X) {
		return Complex.add(Complex.times(X, Complex.times(X, X)), new Complex(-1));
	}
	
	public Complex f_(Complex X) {
		return Complex.times(new Complex(3), Complex.times(X, X));
	}


}
