package org.girino.frac.android.foss;

import android.content.Context;
import android.content.SharedPreferences;

/** Persist Phoenix distortion parameter p (experiment). */
public final class PhoenixParamsStore {

    public static final double DEFAULT_P_RE = -0.5;
    public static final double DEFAULT_P_IM = 0.0;

    private static final String PREFS = "phoenix_params";
    private static final String KEY_P_RE = "pRe";
    private static final String KEY_P_IM = "pIm";

    private PhoenixParamsStore() {
    }

    public static final class Params {
        public final double pRe;
        public final double pIm;

        public Params(double pRe, double pIm) {
            this.pRe = pRe;
            this.pIm = pIm;
        }
    }

    public static Params load(Context context) {
        SharedPreferences prefs = prefs(context);
        return new Params(
                parseDouble(prefs.getString(KEY_P_RE, null), DEFAULT_P_RE),
                parseDouble(prefs.getString(KEY_P_IM, null), DEFAULT_P_IM));
    }

    public static void save(Context context, double pRe, double pIm) {
        if (context == null) {
            return;
        }
        prefs(context)
                .edit()
                .putString(KEY_P_RE, Double.toString(pRe))
                .putString(KEY_P_IM, Double.toString(pIm))
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
