package org.girino.frac.android.foss;

import android.content.Context;
import android.content.SharedPreferences;

/** Persist fractal session across cold start (issue #19). */
public final class SessionStore {

    private static final String PREFS = "fractal_session";
    private static final String KEY_SAVED = "saved";
    private static final String KEY_CENTER_X = "centerX";
    private static final String KEY_CENTER_Y = "centerY";
    private static final String KEY_SCALE = "scale";
    private static final String KEY_VIEW_WIDTH = "viewWidth";
    private static final String KEY_VIEW_HEIGHT = "viewHeight";
    private static final String KEY_OPERATOR_INDEX = "operatorIndex";
    private static final String KEY_PALETTE_INDEX = "paletteIndex";
    private static final String KEY_SMOOTH = "smooth";

    private SessionStore() {
    }

    public static void save(Context context, ViewportSession session) {
        if (context == null || session == null) {
            return;
        }
        prefs(context)
                .edit()
                .putBoolean(KEY_SAVED, true)
                .putString(KEY_CENTER_X, Double.toString(session.centerX))
                .putString(KEY_CENTER_Y, Double.toString(session.centerY))
                .putString(KEY_SCALE, Double.toString(session.scale))
                .putInt(KEY_VIEW_WIDTH, Math.max(1, session.viewWidth))
                .putInt(KEY_VIEW_HEIGHT, Math.max(1, session.viewHeight))
                .putInt(KEY_OPERATOR_INDEX, session.operatorIndex)
                .putInt(KEY_PALETTE_INDEX, session.paletteIndex)
                .putBoolean(KEY_SMOOTH, session.smooth)
                .apply();
    }

    /** Null when nothing was saved yet. */
    public static ViewportSession load(Context context) {
        SharedPreferences prefs = prefs(context);
        if (!prefs.getBoolean(KEY_SAVED, false)) {
            return null;
        }
        int operatorIndex = prefs.getInt(KEY_OPERATOR_INDEX, 0);
        int paletteIndex = prefs.getInt(KEY_PALETTE_INDEX, 0);
        operatorIndex = Math.max(0, Math.min(operatorIndex, FormulaCatalog.size() - 1));
        paletteIndex = Math.max(0, Math.min(paletteIndex, PaletteCatalog.size() - 1));
        return new ViewportSession(
                parseDouble(prefs.getString(KEY_CENTER_X, "0"), 0),
                parseDouble(prefs.getString(KEY_CENTER_Y, "0"), 0),
                parseDouble(prefs.getString(KEY_SCALE, "100"), 100),
                Math.max(1, prefs.getInt(KEY_VIEW_WIDTH, 1)),
                Math.max(1, prefs.getInt(KEY_VIEW_HEIGHT, 1)),
                operatorIndex,
                paletteIndex,
                prefs.getBoolean(KEY_SMOOTH, false));
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
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
