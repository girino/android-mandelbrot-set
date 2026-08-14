package org.girino.frac.operators;


/**
 * This is optimized by removing the Complex class and working 
 * directly with doubles. Less abstraction, much performance.
 * @author girino
 *
 */
public class OptimizedMandelbrotOperator extends FractalOperator {
	
	double x;
	double y;
	double x0;
	double y0;
	protected void beforeIteration(int step, Complex Z, Complex C, int maxiter) {
		x0 = C.getReal();
		y0 = C.getImag();
		x = 0; 
		y = 0;
	}
	protected boolean diverge(int step, Complex Z, Complex C, int maxiter) {
		return (x * x + y * y) < omega2;
	}
	protected void step(int step, Complex Z, Complex C, int maxiter) {
		double t = this.x0 + x * x - y * y;
		y = this.y0 + x * y * 2f;
		x = t;
	}
	protected void afterIteration(int step, Complex Z, Complex C, int maxiter) {
		Z.set(x, y);
	}
}
