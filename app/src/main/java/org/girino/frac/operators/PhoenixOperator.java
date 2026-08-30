package org.girino.frac.operators;

/**
 * Phoenix fractal: z(n+1) = z(n)^2 + c + p*z(n-1).
 */
public class PhoenixOperator extends FractalOperator {

    private static final double DEFAULT_P_RE = -0.5;
    private static final double DEFAULT_P_IM = 0.0;

    private double pRe;
    private double pIm;

    private double prevRe;
    private double prevIm;

    public PhoenixOperator() {
        this(DEFAULT_P_RE, DEFAULT_P_IM);
    }

    public PhoenixOperator(double pRe, double pIm) {
        this.pRe = pRe;
        this.pIm = pIm;
    }

    public double getPRe() {
        return pRe;
    }

    public double getPIm() {
        return pIm;
    }

    @Override
    public boolean orbitCheckpointUsesPrev() {
        return true;
    }

    @Override
    protected void beforeIteration(int step, Complex Z, Complex C, int maxiter) {
        prevRe = 0;
        prevIm = 0;
        Z.set(0, 0);
    }

    @Override
    protected void resumeIterationWithPrev(
            int step, Complex Z, Complex C, int maxiter, double checkpointPrevRe, double checkpointPrevIm) {
        prevRe = checkpointPrevRe;
        prevIm = checkpointPrevIm;
    }

    @Override
    public void readOrbitPrevZ(Complex dest) {
        if (dest != null) {
            dest.set(prevRe, prevIm);
        }
    }

    @Override
    protected void step(int step, Complex Z, Complex C, int maxiter) {
        double zRe = Z.getReal();
        double zIm = Z.getImag();
        double cRe = C.getReal();
        double cIm = C.getImag();
        double newRe = zRe * zRe - zIm * zIm + cRe + pRe * prevRe - pIm * prevIm;
        double newIm = 2 * zRe * zIm + cIm + pRe * prevIm + pIm * prevRe;
        prevRe = zRe;
        prevIm = zIm;
        Z.set(newRe, newIm);
    }
}
