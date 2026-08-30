package org.girino.frac.operators;

/**
 * Julia Phoenix: z(n+1) = z(n)^2 + c + p*z(n-1) with fixed c and p;
 * pixel coordinate is z(0).
 */
public class JuliaPhoenixOperator extends FractalOperator {

    private static final double DEFAULT_C_RE = 0.285;
    private static final double DEFAULT_C_IM = 0.013;
    private static final double DEFAULT_P_RE = -0.5;
    private static final double DEFAULT_P_IM = 0.0;

    private final double cRe;
    private final double cIm;
    private final double pRe;
    private final double pIm;

    private double prevRe;
    private double prevIm;

    public JuliaPhoenixOperator() {
        this(DEFAULT_C_RE, DEFAULT_C_IM, DEFAULT_P_RE, DEFAULT_P_IM);
    }

    public JuliaPhoenixOperator(double cRe, double cIm, double pRe, double pIm) {
        this.cRe = cRe;
        this.cIm = cIm;
        this.pRe = pRe;
        this.pIm = pIm;
    }

    public double getCRe() {
        return cRe;
    }

    public double getCIm() {
        return cIm;
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
        Z.set(C.getReal(), C.getImag());
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
        double newRe = zRe * zRe - zIm * zIm + cRe + pRe * prevRe - pIm * prevIm;
        double newIm = 2 * zRe * zIm + cIm + pRe * prevIm + pIm * prevRe;
        prevRe = zRe;
        prevIm = zIm;
        Z.set(newRe, newIm);
    }
}
