package org.girino.frac.operators;

/**
 * Julia set: z(n+1) = z(n)^2 + c with fixed c; pixel coordinate is z(0).
 */
public class JuliaOperator extends FractalOperator {

    private static final double DEFAULT_C_RE = 0.285;
    private static final double DEFAULT_C_IM = 0.013;

    private final double cRe;
    private final double cIm;

    public JuliaOperator() {
        this(DEFAULT_C_RE, DEFAULT_C_IM);
    }

    public JuliaOperator(double cRe, double cIm) {
        this.cRe = cRe;
        this.cIm = cIm;
    }

    public double getCRe() {
        return cRe;
    }

    public double getCIm() {
        return cIm;
    }

    @Override
    protected void beforeIteration(int step, Complex Z, Complex C, int maxiter) {
        Z.set(C.getReal(), C.getImag());
    }

    @Override
    protected void step(int step, Complex Z, Complex C, int maxiter) {
        double zRe = Z.getReal();
        double zIm = Z.getImag();
        double newRe = zRe * zRe - zIm * zIm + cRe;
        double newIm = 2 * zRe * zIm + cIm;
        Z.set(newRe, newIm);
    }
}
