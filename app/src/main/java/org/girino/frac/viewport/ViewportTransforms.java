package org.girino.frac.viewport;

/**
 * Pure viewport math shared by MandelbrotView and unit tests.
 * Screen to complex mapping matches the fractal renderer:
 * (screen - size/2) / scale + center.
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
     * panX/panY are bitmap-space offsets (screen delta divided by any live preview scale).
     */
    public static State commitPan(State state, float panX, float panY) {
        return new State(
                state.centerX - panX / state.scale,
                state.centerY - panY / state.scale,
                state.scale);
    }

    /**
     * Commits a frozen gesture preview (accumulated scale about an anchor plus
     * translation) into a target viewport, keeping the complex point under the
     * anchor invariant. The preview draws the published bitmap with
     * q = s*p + pos + (1-s)*anchor; the target viewport renders under identity
     * transform. Requiring q(f) to map to the same complex point in both gives:
     * targetCenter = center + (f - pos/s - size/2)/scale - (f - size/2)/(scale*s)
     * per axis. Pure pan (s=1) and centered pinch reduce to commitPan and
     * center-preserving zoom respectively.
     */
    public static State commitFrozenGesture(
            State state,
            float accumulatedScale,
            float positionX,
            float positionY,
            float focusX,
            float focusY,
            int width,
            int height) {
        if (accumulatedScale == 1f) {
            if (positionX == 0f && positionY == 0f) {
                return state;
            }
            return commitPan(state, positionX, positionY);
        }
        double s = accumulatedScale;
        double totalScale = state.scale * s;
        double newCenterX =
                state.centerX
                        + (focusX - positionX / s - width / 2.0) / state.scale
                        - (focusX - width / 2.0) / totalScale;
        double newCenterY =
                state.centerY
                        + (focusY - positionY / s - height / 2.0) / state.scale
                        - (focusY - height / 2.0) / totalScale;
        return new State(newCenterX, newCenterY, totalScale);
    }

    /**
     * Commits a pinch zoom around (focusX, focusY), folding any outstanding
     * pan at the pre-zoom scale first. This keeps the complex point under the focus
     * stable for the transform order used by MandelbrotView (translate then
     * scale-around-focus in geometry space).
     */
    public static State commitPinch(
            State state,
            double factor,
            float focusX,
            float focusY,
            int width,
            int height,
            float panX,
            float panY) {
        if (factor == 1.0) {
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

    /**
     * Canvas preview bridge: stale bitmap at published coordinates drawn with
     * scale-about-center then translate must match target coordinates on screen
     * (same convention as MandelbrotView).
     */
    public static final class PreviewBridge {
        public final float scale;
        public final float focusX;
        public final float focusY;
        public final float posX;
        public final float posY;

        public PreviewBridge(float scale, float focusX, float focusY, float posX, float posY) {
            this.scale = scale;
            this.focusX = focusX;
            this.focusY = focusY;
            this.posX = posX;
            this.posY = posY;
        }
    }

    public static PreviewBridge bridgeFromPublishedToTarget(
            double publishedCenterX,
            double publishedCenterY,
            double publishedScale,
            double targetCenterX,
            double targetCenterY,
            double targetScale,
            int width,
            int height) {
        return new PreviewBridge(
                (float) (targetScale / publishedScale),
                width * 0.5f,
                height * 0.5f,
                (float) ((publishedCenterX - targetCenterX) * publishedScale),
                (float) ((publishedCenterY - targetCenterY) * publishedScale));
    }

    /** Complex coords visible at a screen pixel after applying PreviewBridge on a published bitmap. */
    public static double[] complexAtScreen(
            float screenX,
            float screenY,
            PreviewBridge bridge,
            int width,
            int height,
            double publishedCenterX,
            double publishedCenterY,
            double publishedScale) {
        float bitmapX =
                (screenX - bridge.focusX) / bridge.scale + bridge.focusX - bridge.posX;
        float bitmapY =
                (screenY - bridge.focusY) / bridge.scale + bridge.focusY - bridge.posY;
        return new double[] {
            complexX(bitmapX, width, publishedCenterX, publishedScale),
            complexY(bitmapY, height, publishedCenterY, publishedScale)
        };
    }
}
