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

		public EscapeSample(double value, boolean escaped, int iterations) {
			this.value = value;
			this.escaped = escaped;
			this.iterations = iterations;
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
		return finishSample(i, Z, C, maxiter, isSmooth);
	}

	/**
	 * Continue an orbit from a checkpoint (Adaptive warm-start). When
	 * startIter is 0, behaves like sample().
	 */
	public final EscapeSample sampleContinue(
			Complex C,
			int startIter,
			double zRe,
			double zIm,
			int maxiter,
			boolean isSmooth) {
		if (startIter <= 0) {
			return sample(C, maxiter, isSmooth);
		}
		int i = startIter;
		Z.set(zRe, zIm);
		resumeIteration(i, Z, C, maxiter);
		for (; i < maxiter && diverge(i, Z, C, maxiter); i++) {
			step(i, Z, C, maxiter);
		}
		afterIteration(i, Z, C, maxiter);
		return finishSample(i, Z, C, maxiter, isSmooth);
	}

	/** Copies the orbit Z after sample / sampleContinue (worker-local). */
	public final void readOrbitZ(Complex dest) {
		if (dest != null) {
			dest.set(Z.getReal(), Z.getImag());
		}
	}

	private EscapeSample finishSample(int i, Complex Z, Complex C, int maxiter, boolean isSmooth) {
		boolean escaped = i < maxiter;
		double value = isSmooth
				? produceSmoothResult(i, Z, C, maxiter)
				: produceResult(i, Z, C, maxiter);
		return new EscapeSample(value, escaped, i);
	}

	protected void beforeIteration(int step, Complex Z, Complex C, int maxiter) { ; }

	/**
	 * One-time setup when resuming from a checkpoint (e.g. sync primitive
	 * fields, apply C transforms that beforeIteration would have done once).
	 */
	protected void resumeIteration(int step, Complex Z, Complex C, int maxiter) { ; }

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
