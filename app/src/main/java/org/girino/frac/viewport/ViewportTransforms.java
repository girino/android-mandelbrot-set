package org.girino.frac.viewport;

/**
 * Pure viewport math shared by {@code MandelbrotView} and unit tests.
 * Screen → complex mapping matches the fractal renderer:
 * {@code (screen - size/2) / scale + center}.
 */
public final class ViewportTransforms {
    private ViewportTransforms() {
    }

    public static final class State {
        public final double centerX;
        public final double centerY;
        public final double scale;

        public State(double centerX, double centerY, double scale) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.scale = scale;
        }
    }

    public static double complexX(double screenX, int width, double centerX, double scale) {
        return (screenX - width / 2.0) / scale + centerX;
    }

    public static double complexY(double screenY, int height, double centerY, double scale) {
        return (screenY - height / 2.0) / scale + centerY;
    }

    /**
     * Commits a screen-space pan into the fractal center.
     * Matches canvas translate-then-draw with identity scale.
     */
    public static State commitPan(State state, float panX, float panY) {
        return new State(
                state.centerX - panX / state.scale,
                state.centerY - panY / state.scale,
                state.scale);
    }

    /**
     * Commits a pinch zoom around {@code (focusX, focusY)}, folding any outstanding
     * pan at the pre-zoom scale first. This keeps the complex point under the focus
     * stable for the transform order used by {@code MandelbrotView} (translate then
     * scale-around-focus in geometry space).
     */
    public static State commitPinch(
            State state,
            float factor,
            float focusX,
            float focusY,
            int width,
            int height,
            float panX,
            float panY) {
        if (factor == 1f) {
            if (panX == 0f && panY == 0f) {
                return state;
            }
            return commitPan(state, panX, panY);
        }

        State afterPan = commitPan(state, panX, panY);
        double oldScale = afterPan.scale;
        double newScale = oldScale * factor;
        double newCenterX =
                afterPan.centerX
                        + (focusX - width / 2.0) * (1.0 - 1.0 / factor) / oldScale;
        double newCenterY =
                afterPan.centerY
                        + (focusY - height / 2.0) * (1.0 - 1.0 / factor) / oldScale;
        return new State(newCenterX, newCenterY, newScale);
    }
}
