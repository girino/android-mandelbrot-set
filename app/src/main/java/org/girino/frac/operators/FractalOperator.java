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

	/**
	 * Escape-time sample: palette value plus whether the orbit escaped
	 * before hitting maxiter (issue #28 adaptive border refinement).
	 */
	public static final class EscapeSample {
		public final double value;
		/** True when the orbit escaped (left the set) before maxiter. */
		public final boolean escaped;
		/** Iterations performed (maxiter means still interior). */
		public final int iterations;
		/**
		 * Unnormalized escape measure for palette remapping: discrete step
		 * or continuous smooth count. 0 when not escaped.
		 */
		public final double rawCount;

		public EscapeSample(double value, boolean escaped, int iterations, double rawCount) {
			this.value = value;
			this.escaped = escaped;
			this.iterations = iterations;
			this.rawCount = rawCount;
		}
	}

	private Complex Z = new Complex();
	public final double apply(Complex C, int maxiter, boolean isSmooth) {
		return sample(C, maxiter, isSmooth).value;
	}

	public final EscapeSample sample(Complex C, int maxiter, boolean isSmooth) {
		int i = 0;
		Z.set(0, 0);
		beforeIteration(i, Z, C, maxiter);
		for (i = 0; i < maxiter && diverge(i, Z, C, maxiter); i++) {
			step(i, Z, C, maxiter);
		}
		afterIteration(i, Z, C, maxiter);
		boolean escaped = i < maxiter;
		if (isSmooth) {
			double raw = 0.0;
			double value = 0.0;
			if (escaped) {
				raw = i - (Math.log(Math.log(Complex.modulus(Z)))) / getLogEscapeRadius();
				if (raw < 0) {
					raw = 0;
				}
				value = raw / (double) maxiter;
				if (value >= 1.0) {
					raw = 0;
					value = 0;
				}
			}
			return new EscapeSample(value, escaped, i, raw);
		}
		double value = produceResult(i, Z, C, maxiter);
		double raw = escaped ? (double) i : 0.0;
		return new EscapeSample(value, escaped, i, raw);
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
			// For z(n+1) = z(n) ^ k + c, use the renormalized iteration count.
			mu = step - (Math.log(Math.log(Complex.modulus(Z)))) / getLogEscapeRadius();
			mu /= (double)maxiter;
			if (mu >= 1.0) {
				mu = 0;
			}
			if (mu < 0) {
				mu = 0;
			}
		}
		return mu;
	}
}
