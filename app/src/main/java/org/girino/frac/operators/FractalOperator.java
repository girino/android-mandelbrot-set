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
	 *
	 * Issue #33 (2026-08): fields are mutable so render workers reuse one
	 * EscapeSample per row band / border slice instead of allocating per pixel.
	 * No measurable FPS gain on device; no regression either. To revert:
	 * - make value/escaped/iterations final again and drop set()
	 * - remove sampleInto / sampleContinueInto; finishSample returns new EscapeSample
	 * - in ParallelStepRenderer.fillRowRange and AdaptiveRefiner.retestRange,
	 *   call operator.sample / sampleContinue per pixel again
	 * - drop applyScratch; apply() reads sample(...).value
	 * See git tag v1.1.0 (pre-#33) or commit before merge of feature/reuse-escape-sample.
	 */
	public static final class EscapeSample {
		public double value;
		/** True when the orbit escaped (left the set) before maxiter. */
		public boolean escaped;
		/** Iterations performed (maxiter means still interior). */
		public int iterations;

		public EscapeSample() {
		}

		public EscapeSample(double value, boolean escaped, int iterations) {
			set(value, escaped, iterations);
		}

		public void set(double value, boolean escaped, int iterations) {
			this.value = value;
			this.escaped = escaped;
			this.iterations = iterations;
		}
	}

	private Complex Z = new Complex();
	/** Reused by apply() only; not shared across sample() callers. */
	private final EscapeSample applyScratch = new EscapeSample();

	public final double apply(Complex C, int maxiter, boolean isSmooth) {
		sampleInto(C, maxiter, isSmooth, applyScratch);
		return applyScratch.value;
	}

	/**
	 * Allocating convenience API for tests and one-off callers.
	 * Hot path should use sampleInto with a worker-local holder.
	 */
	public final EscapeSample sample(Complex C, int maxiter, boolean isSmooth) {
		return sampleInto(C, maxiter, isSmooth, new EscapeSample());
	}

	/** Writes the escape-time result into out (no allocation). */
	public final EscapeSample sampleInto(Complex C, int maxiter, boolean isSmooth, EscapeSample out) {
		int i = 0;
		Z.set(0, 0);
		beforeIteration(i, Z, C, maxiter);
		for (i = 0; i < maxiter && diverge(i, Z, C, maxiter); i++) {
			step(i, Z, C, maxiter);
		}
		afterIteration(i, Z, C, maxiter);
		return finishSample(i, Z, C, maxiter, isSmooth, out);
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
		return sampleContinueInto(C, startIter, zRe, zIm, maxiter, isSmooth, new EscapeSample());
	}

	/** Continues an orbit into out (no allocation). */
	public final EscapeSample sampleContinueInto(
			Complex C,
			int startIter,
			double zRe,
			double zIm,
			int maxiter,
			boolean isSmooth,
			EscapeSample out) {
		if (startIter <= 0) {
			return sampleInto(C, maxiter, isSmooth, out);
		}
		int i = startIter;
		Z.set(zRe, zIm);
		resumeIteration(i, Z, C, maxiter);
		for (; i < maxiter && diverge(i, Z, C, maxiter); i++) {
			step(i, Z, C, maxiter);
		}
		afterIteration(i, Z, C, maxiter);
		return finishSample(i, Z, C, maxiter, isSmooth, out);
	}

	/** Copies the orbit Z after sample / sampleContinue (worker-local). */
	public final void readOrbitZ(Complex dest) {
		if (dest != null) {
			dest.set(Z.getReal(), Z.getImag());
		}
	}

	private EscapeSample finishSample(
			int i, Complex Z, Complex C, int maxiter, boolean isSmooth, EscapeSample out) {
		boolean escaped = i < maxiter;
		double value = isSmooth
				? produceSmoothResult(i, Z, C, maxiter)
				: produceResult(i, Z, C, maxiter);
		out.set(value, escaped, i);
		return out;
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
