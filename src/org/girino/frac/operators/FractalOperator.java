package org.girino.frac.operators;


/**
 * Implement this interface to calculate the fractal 
 * escape velocity for a given fractal set (i.e. Mandelbrot).
 * 
 * @author girino
 */
public abstract class FractalOperator {
	protected static final double omega = 4;
	protected static final double omega2 = omega*omega;
	private static final double logEscapeRadius = Math.log(2.0);

	private Complex Z = new Complex();
	public final double apply(Complex C, int maxiter, boolean isSmooth) {
		int i = 0;
		Z.set(0, 0);
		beforeIteration(i, Z, C, maxiter);
		for (i = 0; i < maxiter && diverge(i, Z, C, maxiter); i++) {
			step(i, Z, C, maxiter);
		}
		afterIteration(i, Z, C, maxiter);
		
		if (isSmooth) {
			return produceSmoothResult(i, Z, C, maxiter);
		} else {
			return produceResult(i, Z, C, maxiter);
		}
	}
	protected void beforeIteration(int step, Complex Z, Complex C, int maxiter) { ; }
	protected void afterIteration(int step, Complex Z, Complex C, int maxiter) { ; }
	abstract protected void step(int step, Complex Z, Complex C, int maxiter);
	protected boolean diverge(int step, Complex Z, Complex C, int maxiter) {
		return (Z.modulus2() <= omega2);
	}
	protected double produceResult(int step, Complex Z, Complex C, int maxiter) {
		return ((double)step)/((double)maxiter);
	}
	protected double getLogEscapeRadius() {
		return logEscapeRadius;
	}
	protected double produceSmoothResult(int step, Complex Z, Complex C, int maxiter) {
		double mu = 0.0;
		if (step < maxiter) {
		    // For the more general formula z(n+1) = z(n) ^ k + c, the renormalized count is
		    // mu = N + 1 - log (log  |Z(N)|) / log k
		    mu = step - (Math.log(Math.log(Complex.modulus(Z)))) / getLogEscapeRadius();
		    mu /= (double)maxiter;
			if (mu >= 1.0) mu = 0;
			if (mu < 0) mu = 0;
		}
		return mu;
	}
}
