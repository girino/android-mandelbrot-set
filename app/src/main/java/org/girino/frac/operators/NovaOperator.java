package org.girino.frac.operators;


/**
 * operator for mandelbrot sets.. 
 * 
 * much simple than buddhabrot, huh?
 * @author girino
 *
 */
public class NovaOperator extends FractalOperator {
	
	private static final Complex TWO = new Complex(2);

    Complex tmpf = new Complex();
    Complex tmpf_ = new Complex();
    Complex tmp2 = new Complex();
    Complex tmp3 = new Complex();
    Complex C = new Complex();
    Complex Z = new Complex();

    protected void beforeIteration(int step, Complex Z, Complex C, int maxiter) {
	    Z.set(4,0);
    }
	/**
	 *     Fractal Geometry: Nova Fractal
	 *     z(n) := z(n-1) - (2 * f(z(n-1))/f’(z(n-1))) + c. 
	 *     Where z ∈ C, c ∈ C and f(z)=z^3-1.
	 *
	 */
    protected void step(int step, Complex Z, Complex C, int maxiter) {
		double a = Z.getReal(); double a2 = a*a; double a3 = a2*a; double a4 = a3*a; double a5 = a4*a;
		double b = Z.getImag(); double b2 = b*b; double b3 = b2*b; double b4 = b3*b; double b5 = b4*b;
		double twoOverA2plusb2squaretimes3 = 2.0/(3.0*((a2 + b2) * (a2 + b2))); 
		double k = C.getReal(); 
		double j = C.getImag(); 
	    double zr = a + (a2 - a5 - b2 - (2*a3*b2) - (a*b4))*(twoOverA2plusb2squaretimes3) + k;
		double zi = b - ((2*a*b) + (2*a2*b3) + (a4*b) + b5)*(twoOverA2plusb2squaretimes3) + j;
		
		Z.set(zr, zi);
	}
	
	protected void novaFunction() {
		f(tmpf, Z);
		f_(tmpf_, Z);
		tmpf_.inverse();
		Complex tmp = Complex.times(tmpf, tmpf_);
//	    	Complex tmp = Complex.times(fold(Z), Complex.inverse(f_old(Z)));
		//System.out.println(f(Z));
		tmp2.ltimes(TWO, tmp);
		tmp2.minus();
		//System.out.println(tmp2);
		tmp3.ladd(tmp2, C);
		//System.out.println(tmp3);
		//System.out.println(Z.toString() + " - " + Complex.add(Z, tmp3).toString());
		Z.ladd(Z, tmp3);
	}
	
	public static final Complex MINUS_1 = new Complex(-1);
	public void f(Complex ret, Complex X) {
		// X^3 - 1
		ret.set(X);
		ret.square();
		ret.ltimes(ret, X); // ret = X^3
		ret.ladd(ret, MINUS_1);
		ret.ladd(Complex.times(X, Complex.times(X, X)), new Complex(-1));
	}
	
	public static final Complex THREE = new Complex(3);
	public void f_(Complex ret, Complex X) {
		// 3 x^2
		ret.lsquare(X);
		ret.ltimes(THREE, ret);
	}

	public Complex fold(Complex X) {
		return Complex.add(Complex.times(X, Complex.times(X, X)), new Complex(-1));
	}
	
	public Complex f_old(Complex X) {
		return Complex.times(new Complex(3), Complex.times(X, X));
	}

	protected Complex novaFunctionOld(Complex Z, Complex C) {
		Complex tmp = Complex.times(fold(Z), Complex.inverse(f_old(Z)));
		//System.out.println(f(Z));
		Complex tmp2 = Complex.times(new Complex(2), tmp);
		//System.out.println(tmp2);
		Complex tmp3 = Complex.add(Complex.minus(tmp2), C);
		//System.out.println(tmp3);
		//System.out.println(Z.toString() + " - " + Complex.add(Z, tmp3).toString());
		Z = Complex.add(Z, tmp3);
		return Z;
	}
	
}
