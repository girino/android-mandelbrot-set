package org.girino.frac.android.foss;

/**
 * User-chosen escape-time iteration policy (issues #26 / #28).
 * FIXED uses fixedMax for every pixel; SCALE_WITH_ZOOM uses baseMax and
 * multiplier against viewport scale (see IterationPolicy); ADAPTIVE uses
 * fixedMax for pass 1 then border-doubling refinement up to maxRounds /
 * absoluteCap (see AdaptiveRefiner).
 */
public final class IterationSettings {

    public enum Mode {
        FIXED,
        SCALE_WITH_ZOOM,
        ADAPTIVE
    }

    public static final int MIN_ITER = 10;
    public static final int MAX_ITER_CAP = 4096;
    public static final int DEFAULT_FIXED_MAX = 40;
    public static final int DEFAULT_BASE_MAX = 40;
    public static final double DEFAULT_MULTIPLIER = 1.2;
    public static final int DEFAULT_MAX_ROUNDS = 8;
    public static final int DEFAULT_ABSOLUTE_CAP = MAX_ITER_CAP;
    public static final int MIN_ROUNDS = 1;
    public static final int MAX_ROUNDS = 16;
    public static final double MIN_MULTIPLIER = 1.01;
    public static final double MAX_MULTIPLIER = 4.0;

    public final Mode mode;
    public final int fixedMax;
    public final int baseMax;
    public final double multiplier;
    /** Adaptive: max border-doubling rounds after pass 1. */
    public final int maxRounds;
    /** Adaptive: hard ceiling for iteration limit across rounds. */
    public final int absoluteCap;

    public IterationSettings(Mode mode, int fixedMax, int baseMax, double multiplier) {
        this(mode, fixedMax, baseMax, multiplier, DEFAULT_MAX_ROUNDS, DEFAULT_ABSOLUTE_CAP);
    }

    public IterationSettings(
            Mode mode,
            int fixedMax,
            int baseMax,
            double multiplier,
            int maxRounds,
            int absoluteCap) {
        this.mode = mode != null ? mode : Mode.FIXED;
        this.fixedMax = clampInt(fixedMax, MIN_ITER, MAX_ITER_CAP);
        this.baseMax = clampInt(baseMax, MIN_ITER, MAX_ITER_CAP);
        this.multiplier = clampDouble(multiplier, MIN_MULTIPLIER, MAX_MULTIPLIER);
        this.maxRounds = clampInt(maxRounds, MIN_ROUNDS, MAX_ROUNDS);
        this.absoluteCap = clampInt(absoluteCap, MIN_ITER, MAX_ITER_CAP);
    }

    public static IterationSettings defaults() {
        return new IterationSettings(
                Mode.FIXED,
                DEFAULT_FIXED_MAX,
                DEFAULT_BASE_MAX,
                DEFAULT_MULTIPLIER,
                DEFAULT_MAX_ROUNDS,
                DEFAULT_ABSOLUTE_CAP);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
