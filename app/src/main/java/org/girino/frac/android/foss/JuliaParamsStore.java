package org.girino.frac.android.foss;

import android.content.Context;
import android.content.SharedPreferences;

/** Persist Julia seed c (fixed parameter). */
public final class JuliaParamsStore {

    public static final double DEFAULT_C_RE = 0.285;
    public static final double DEFAULT_C_IM = 0.013;

    private static final String PREFS = "julia_params";
    private static final String KEY_C_RE = "cRe";
    private static final String KEY_C_IM = "cIm";

    private JuliaParamsStore() {
    }

    public static final class Params {
        public final double cRe;
        public final double cIm;

        public Params(double cRe, double cIm) {
            this.cRe = cRe;
            this.cIm = cIm;
        }
    }

    public static Params load(Context context) {
        SharedPreferences prefs = prefs(context);
        return new Params(
                parseDouble(prefs.getString(KEY_C_RE, null), DEFAULT_C_RE),
                parseDouble(prefs.getString(KEY_C_IM, null), DEFAULT_C_IM));
    }

    public static void save(Context context, double cRe, double cIm) {
        if (context == null) {
            return;
        }
        prefs(context)
                .edit()
                .putString(KEY_C_RE, Double.toString(cRe))
                .putString(KEY_C_IM, Double.toString(cIm))
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static double parseDouble(String raw, double fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
