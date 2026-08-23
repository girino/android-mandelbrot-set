package org.girino.frac.android.foss;

import android.content.Context;
import android.content.SharedPreferences;

/** Persist IterationSettings in SharedPreferences (issue #26). */
public final class IterationSettingsStore {

    private static final String PREFS = "iteration_settings";
    private static final String KEY_MODE = "mode";
    private static final String KEY_FIXED = "fixed_max";
    private static final String KEY_BASE = "base_max";
    private static final String KEY_MULTIPLIER = "multiplier";

    private IterationSettingsStore() {
    }

    public static IterationSettings load(Context context) {
        SharedPreferences prefs = prefs(context);
        IterationSettings.Mode mode = parseMode(prefs.getString(KEY_MODE, null));
        int fixed = prefs.getInt(KEY_FIXED, IterationSettings.DEFAULT_FIXED_MAX);
        int base = prefs.getInt(KEY_BASE, IterationSettings.DEFAULT_BASE_MAX);
        float multiplier = prefs.getFloat(
                KEY_MULTIPLIER,
                (float) IterationSettings.DEFAULT_MULTIPLIER);
        return new IterationSettings(mode, fixed, base, multiplier);
    }

    public static void save(Context context, IterationSettings settings) {
        if (settings == null) {
            return;
        }
        prefs(context)
                .edit()
                .putString(KEY_MODE, settings.mode.name())
                .putInt(KEY_FIXED, settings.fixedMax)
                .putInt(KEY_BASE, settings.baseMax)
                .putFloat(KEY_MULTIPLIER, (float) settings.multiplier)
                .apply();
    }

    /** Restore factory defaults and persist (issue #26). */
    public static IterationSettings resetToDefaults(Context context) {
        IterationSettings defaults = IterationSettings.defaults();
        save(context, defaults);
        return defaults;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static IterationSettings.Mode parseMode(String raw) {
        if (raw == null) {
            return IterationSettings.Mode.FIXED;
        }
        try {
            return IterationSettings.Mode.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return IterationSettings.Mode.FIXED;
        }
    }
}
