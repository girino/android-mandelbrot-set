package org.girino.frac.android.foss;

/**
 * User-chosen escape-time iteration policy (issue #26).
 * FIXED uses fixedMax for every pixel; SCALE_WITH_ZOOM uses baseMax and
 * multiplier against viewport scale (see IterationPolicy).
 */
public final class IterationSettings {

    public enum Mode {
        FIXED,
        SCALE_WITH_ZOOM
    }

    public static final int DEFAULT_FIXED_MAX = 40;
    public static final int DEFAULT_BASE_MAX = 40;
    public static final double DEFAULT_MULTIPLIER = 1.2;
    public static final int MIN_ITER = 10;
    public static final int MAX_ITER_CAP = 4096;
    public static final double MIN_MULTIPLIER = 1.01;
    public static final double MAX_MULTIPLIER = 4.0;

    public final Mode mode;
    public final int fixedMax;
    public final int baseMax;
    public final double multiplier;

    public IterationSettings(Mode mode, int fixedMax, int baseMax, double multiplier) {
        this.mode = mode != null ? mode : Mode.FIXED;
        this.fixedMax = clampInt(fixedMax, MIN_ITER, MAX_ITER_CAP);
        this.baseMax = clampInt(baseMax, MIN_ITER, MAX_ITER_CAP);
        this.multiplier = clampDouble(multiplier, MIN_MULTIPLIER, MAX_MULTIPLIER);
    }

    public static IterationSettings defaults() {
        return new IterationSettings(
                Mode.FIXED,
                DEFAULT_FIXED_MAX,
                DEFAULT_BASE_MAX,
                DEFAULT_MULTIPLIER);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
