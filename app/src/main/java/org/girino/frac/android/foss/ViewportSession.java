package org.girino.frac.android.foss;

import android.os.Bundle;

/** Saved fractal session for configuration changes (issue #21). */
public final class ViewportSession {

    private static final String KEY_CENTER_X = "centerX";
    private static final String KEY_CENTER_Y = "centerY";
    private static final String KEY_SCALE = "scale";
    private static final String KEY_VIEW_WIDTH = "viewWidth";
    private static final String KEY_VIEW_HEIGHT = "viewHeight";
    private static final String KEY_OPERATOR_INDEX = "operatorIndex";
    private static final String KEY_PALETTE_INDEX = "paletteIndex";
    private static final String KEY_SMOOTH = "smooth";

    public final double centerX;
    public final double centerY;
    public final double scale;
    public final int viewWidth;
    public final int viewHeight;
    public final int operatorIndex;
    public final int paletteIndex;
    public final boolean smooth;

    public ViewportSession(
            double centerX,
            double centerY,
            double scale,
            int viewWidth,
            int viewHeight,
            int operatorIndex,
            int paletteIndex,
            boolean smooth) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.scale = scale;
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.operatorIndex = operatorIndex;
        this.paletteIndex = paletteIndex;
        this.smooth = smooth;
    }

    Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putDouble(KEY_CENTER_X, centerX);
        bundle.putDouble(KEY_CENTER_Y, centerY);
        bundle.putDouble(KEY_SCALE, scale);
        bundle.putInt(KEY_VIEW_WIDTH, viewWidth);
        bundle.putInt(KEY_VIEW_HEIGHT, viewHeight);
        bundle.putInt(KEY_OPERATOR_INDEX, operatorIndex);
        bundle.putInt(KEY_PALETTE_INDEX, paletteIndex);
        bundle.putBoolean(KEY_SMOOTH, smooth);
        return bundle;
    }

    static ViewportSession fromBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        return new ViewportSession(
                bundle.getDouble(KEY_CENTER_X),
                bundle.getDouble(KEY_CENTER_Y),
                bundle.getDouble(KEY_SCALE),
                bundle.getInt(KEY_VIEW_WIDTH),
                bundle.getInt(KEY_VIEW_HEIGHT),
                bundle.getInt(KEY_OPERATOR_INDEX),
                bundle.getInt(KEY_PALETTE_INDEX),
                bundle.getBoolean(KEY_SMOOTH));
    }
}
