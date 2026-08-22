package org.girino.frac.viewport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Regression for GitHub issue #2: pinch zoom must keep the complex point under
 * the gesture focus stable (no abrupt viewport jump).
 */
class ViewportTransformsTest {

    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1920;
    private static final double EPS = 1e-12;

    @Test
    void complexMappingMatchesRendererConvention() {
        assertEquals(
                0.25,
                ViewportTransforms.complexX(WIDTH / 2.0 + 50, WIDTH, 0.0, 200.0),
                EPS);
        assertEquals(
                -0.1,
                ViewportTransforms.complexY(HEIGHT / 2.0 - 20, HEIGHT, 0.0, 200.0),
                EPS);
    }

    @Test
    void panKeepsDraggedScreenPointFixed() {
        ViewportTransforms.State before = new ViewportTransforms.State(-0.5, 0.2, 150.0);
        float panX = 80f;
        float panY = -40f;
        float screenX = 300f;
        float screenY = 900f;

        // With identity scale preview, screen S shows bitmap S - pan.
        double complexBefore = ViewportTransforms.complexX(
                screenX - panX, WIDTH, before.centerX, before.scale);
        ViewportTransforms.State after = ViewportTransforms.commitPan(before, panX, panY);
        double complexAfter =
                ViewportTransforms.complexX(screenX, WIDTH, after.centerX, after.scale);

        assertEquals(complexBefore, complexAfter, EPS);
        assertEquals(
                ViewportTransforms.complexY(screenY - panY, HEIGHT, before.centerY, before.scale),
                ViewportTransforms.complexY(screenY, HEIGHT, after.centerY, after.scale),
                EPS);
    }

    @ParameterizedTest
    @CsvSource({
            // focusX, focusY, factor, panX, panY, centerX, centerY, scale
            "540, 960, 2.0, 0, 0, 0.0, 0.0, 100.0",
            "100, 200, 1.5, 0, 0, -0.75, 0.1, 180.0",
            "900, 1600, 0.5, 0, 0, 0.2, -0.3, 220.0",
            "200, 300, 2.5, 40, -25, -0.1, 0.05, 160.0",
            "800, 100, 0.75, -30, 50, 0.0, 0.0, 120.0"
    })
    void pinchKeepsComplexPointUnderFocusFixed(
            float focusX,
            float focusY,
            float factor,
            float panX,
            float panY,
            double centerX,
            double centerY,
            double scale) {
        ViewportTransforms.State before =
                new ViewportTransforms.State(centerX, centerY, scale);

        // Preview: geometry sees translate(pan) then scale-around-focus.
        // Screen focus F maps to bitmap B = F - pan / factor... wait.
        // Canvas calls: scale(s, F); translate(pan). Applied to geometry: translate then scale.
        // screen = F + s * ((B + pan) - F)
        // At screen = F: B = F - pan
        // So complex under focus uses bitmap coord (focus - pan) at old scale.
        double beforeX = ViewportTransforms.complexX(
                focusX - panX, WIDTH, before.centerX, before.scale);
        double beforeY = ViewportTransforms.complexY(
                focusY - panY, HEIGHT, before.centerY, before.scale);

        ViewportTransforms.State after = ViewportTransforms.commitPinch(
                before, factor, focusX, focusY, WIDTH, HEIGHT, panX, panY);

        double afterX =
                ViewportTransforms.complexX(focusX, WIDTH, after.centerX, after.scale);
        double afterY =
                ViewportTransforms.complexY(focusY, HEIGHT, after.centerY, after.scale);

        assertEquals(beforeX, afterX, EPS, "real under focus jumped");
        assertEquals(beforeY, afterY, EPS, "imag under focus jumped");
        assertEquals(scale * factor, after.scale, EPS);
    }

    @Test
    void factorOneWithPanOnlyCommitsPan() {
        ViewportTransforms.State before = new ViewportTransforms.State(0.1, -0.2, 90.0);
        ViewportTransforms.State after =
                ViewportTransforms.commitPinch(before, 1f, 10f, 20f, WIDTH, HEIGHT, 15f, -5f);
        ViewportTransforms.State panOnly = ViewportTransforms.commitPan(before, 15f, -5f);

        assertEquals(panOnly.centerX, after.centerX, EPS);
        assertEquals(panOnly.centerY, after.centerY, EPS);
        assertEquals(panOnly.scale, after.scale, EPS);
    }

    @Test
    void aggressiveZoomOutKeepsCornerFocusFixed() {
        ViewportTransforms.State before = new ViewportTransforms.State(-0.75, 0.1, 400.0);
        float focusX = 120f;
        float focusY = 280f;
        double factor = 0.35;

        double beforeX = ViewportTransforms.complexX(
                focusX, WIDTH, before.centerX, before.scale);
        double beforeY = ViewportTransforms.complexY(
                focusY, HEIGHT, before.centerY, before.scale);

        ViewportTransforms.State after = ViewportTransforms.commitPinch(
                before, factor, focusX, focusY, WIDTH, HEIGHT, 0f, 0f);

        assertEquals(beforeX, ViewportTransforms.complexX(
                focusX, WIDTH, after.centerX, after.scale), EPS);
        assertEquals(beforeY, ViewportTransforms.complexY(
                focusY, HEIGHT, after.centerY, after.scale), EPS);
        assertEquals(before.scale * factor, after.scale, EPS);
    }
    @Test
    void movingPivotDiffersFromFixedPivotCommit() {
        ViewportTransforms.State start = new ViewportTransforms.State(0.0, 0.0, 200.0);
        float initialPivotX = 400f;
        float initialPivotY = 800f;
        float driftedPivotX = 520f;
        float driftedPivotY = 920f;
        double factor = 0.6;

        ViewportTransforms.State fixedPivot = ViewportTransforms.commitPinch(
                start, factor, initialPivotX, initialPivotY, WIDTH, HEIGHT, 0f, 0f);
        ViewportTransforms.State driftedPivot = ViewportTransforms.commitPinch(
                start, factor, driftedPivotX, driftedPivotY, WIDTH, HEIGHT, 0f, 0f);

        double fixedComplex = ViewportTransforms.complexX(
                initialPivotX, WIDTH, fixedPivot.centerX, fixedPivot.scale);
        double driftedAtInitial = ViewportTransforms.complexX(
                initialPivotX, WIDTH, driftedPivot.centerX, driftedPivot.scale);
        assertEquals(
                ViewportTransforms.complexX(initialPivotX, WIDTH, start.centerX, start.scale),
                fixedComplex,
                EPS);
        // Using the drifted focus at commit shifts what appears under the initial pinch point.
        assertEquals(
                (driftedPivotX - initialPivotX) * (1.0 - 1.0 / factor) / start.scale,
                driftedAtInitial - fixedComplex,
                EPS);
    }

    @Test
    void panWithActivePreviewScaleUsesBitmapSpaceOffset() {
        ViewportTransforms.State before = new ViewportTransforms.State(0.0, 0.0, 200.0);
        float previewScale = 0.5f;
        float screenDrag = 100f;
        float bitmapPan = screenDrag / previewScale;

        ViewportTransforms.State after = ViewportTransforms.commitPan(before, bitmapPan, 0f);
        assertEquals(-bitmapPan / before.scale, after.centerX, EPS);
    }

    @Test
    void oldCenterZoomWouldMoveCornerFocus_regressionGuard() {
        // Documents the bug from issue #2: scaling scale only (center zoom) moves the
        // complex point under a corner focus; commitPinch must not.
        ViewportTransforms.State before = new ViewportTransforms.State(0.0, 0.0, 100.0);
        float focusX = 50f;
        float focusY = 50f;
        double factor = 2.0;

        double underFocusBefore =
                ViewportTransforms.complexX(focusX, WIDTH, before.centerX, before.scale);

        // Buggy approach: scale *= factor, keep center.
        double buggyAfter =
                ViewportTransforms.complexX(focusX, WIDTH, before.centerX, before.scale * factor);
        assertEquals(underFocusBefore / factor, buggyAfter, EPS);

        ViewportTransforms.State fixed = ViewportTransforms.commitPinch(
                before, factor, focusX, focusY, WIDTH, HEIGHT, 0f, 0f);
        assertEquals(
                underFocusBefore,
                ViewportTransforms.complexX(focusX, WIDTH, fixed.centerX, fixed.scale),
                EPS);
    }

    @Test
    void chainedPinchMustUseIncrementalFactorOnPendingBase_notTotalPreviewScale() {
        ViewportTransforms.State committed = new ViewportTransforms.State(0.1, -0.2, 200.0);
        float firstPinchFactor = 2.0f;
        float secondPinchFactor = 1.5f;
        float focusX = 480f;
        float focusY = 1200f;

        ViewportTransforms.State afterFirst = ViewportTransforms.commitPinch(
                committed, firstPinchFactor, focusX, focusY, WIDTH, HEIGHT, 0f, 0f);

        ViewportTransforms.State correctSecond = ViewportTransforms.commitPinch(
                afterFirst, secondPinchFactor, focusX, focusY, WIDTH, HEIGHT, 0f, 0f);

        ViewportTransforms.State doubleCounted = ViewportTransforms.commitPinch(
                afterFirst,
                firstPinchFactor * secondPinchFactor,
                focusX,
                focusY,
                WIDTH,
                HEIGHT,
                0f,
                0f);

        assertEquals(600.0, correctSecond.scale, EPS);
        assertEquals(1200.0, doubleCounted.scale, EPS);

        double underFocus = ViewportTransforms.complexX(
                focusX, WIDTH, afterFirst.centerX, afterFirst.scale);
        assertEquals(
                underFocus,
                ViewportTransforms.complexX(focusX, WIDTH, correctSecond.centerX, correctSecond.scale),
                EPS);
        assertEquals(
                firstPinchFactor * secondPinchFactor / secondPinchFactor,
                doubleCounted.scale / correctSecond.scale,
                EPS,
                "double-counting applies total preview factor again on pending base");
    }

    @Test
    void chainedPanMustUseIncrementalOffsetOnPendingBase_notTotalPreviewPan() {
        ViewportTransforms.State committed = new ViewportTransforms.State(0.0, 0.0, 180.0);
        float firstPanX = 60f;
        float secondPanX = 35f;

        ViewportTransforms.State afterFirst =
                ViewportTransforms.commitPan(committed, firstPanX, 0f);
        ViewportTransforms.State correctSecond =
                ViewportTransforms.commitPan(afterFirst, secondPanX, 0f);

        ViewportTransforms.State doubleCounted =
                ViewportTransforms.commitPan(afterFirst, firstPanX + secondPanX, 0f);

        assertEquals(
                committed.centerX - (firstPanX + secondPanX) / committed.scale,
                correctSecond.centerX,
                EPS);
        assertEquals(
                -firstPanX / afterFirst.scale,
                doubleCounted.centerX - correctSecond.centerX,
                EPS);
    }

    @Test
    void previewBridgeMatchesTargetViewportOnScreen() {
        double pubCx = 0.1;
        double pubCy = -0.05;
        double pubScale = 150.0;
        ViewportTransforms.State target =
                ViewportTransforms.commitPinch(
                        new ViewportTransforms.State(pubCx, pubCy, pubScale),
                        1.8,
                        200f,
                        900f,
                        WIDTH,
                        HEIGHT,
                        30f,
                        -15f);

        ViewportTransforms.PreviewBridge bridge =
                ViewportTransforms.bridgeFromPublishedToTarget(
                        pubCx, pubCy, pubScale,
                        target.centerX, target.centerY, target.scale,
                        WIDTH, HEIGHT);

        for (float[] point : new float[][] {
            {WIDTH * 0.5f, HEIGHT * 0.5f},
            {200f, 900f},
            {100f, 300f},
            {900f, 1600f}
        }) {
            double[] preview =
                    ViewportTransforms.complexAtScreen(
                            point[0],
                            point[1],
                            bridge,
                            WIDTH,
                            HEIGHT,
                            pubCx,
                            pubCy,
                            pubScale);
            assertEquals(
                    ViewportTransforms.complexX(point[0], WIDTH, target.centerX, target.scale),
                    preview[0],
                    1e-5,
                    "x at " + point[0] + "," + point[1]);
            assertEquals(
                    ViewportTransforms.complexY(point[1], HEIGHT, target.centerY, target.scale),
                    preview[1],
                    1e-5,
                    "y at " + point[0] + "," + point[1]);
        }
    }

    @Test
    void pinchWithMidpointDragMatchesCommitPanThenScale() {
        ViewportTransforms.State start = new ViewportTransforms.State(0.2, -0.1, 200.0);
        float pivotX = 480f;
        float pivotY = 1200f;
        float factor = 1.6f;
        float dragX = 55f;
        float dragY = -30f;

        ViewportTransforms.State fromCommit =
                ViewportTransforms.commitPinch(
                        start, factor, pivotX, pivotY, WIDTH, HEIGHT, dragX, dragY);

        ViewportTransforms.State fromPanThenZoom =
                ViewportTransforms.commitPinch(
                        ViewportTransforms.commitPan(start, dragX, dragY),
                        factor,
                        pivotX,
                        pivotY,
                        WIDTH,
                        HEIGHT,
                        0f,
                        0f);

        assertEquals(fromPanThenZoom.centerX, fromCommit.centerX, EPS);
        assertEquals(fromPanThenZoom.centerY, fromCommit.centerY, EPS);
        assertEquals(fromPanThenZoom.scale, fromCommit.scale, EPS);
    }

    @ParameterizedTest
    @CsvSource({
            // focusX, focusY, accScale, posX, posY — off-center pinch with drag
            "150, 300, 2.0, 20.0, -10.0",
            "900, 1600, 0.5, -35.0, 25.0",
            "80, 1800, 1.75, 0.0, 0.0",
            "1000, 120, 0.4, 60.0, 40.0"
    })
    void frozenGestureKeepsComplexPointUnderFocusFixed(
            float focusX,
            float focusY,
            float accumulatedScale,
            float positionX,
            float positionY) {
        ViewportTransforms.State published =
                new ViewportTransforms.State(-0.3, 0.15, 140.0);

        // Preview draws the published bitmap as q = s*p + pos + (1-s)*focus.
        // The bitmap point displayed at the focus is therefore p(focus).
        float bitmapAtFocusX = (focusX - positionX - (1f - accumulatedScale) * focusX)
                / accumulatedScale;
        double complexBeforeX = ViewportTransforms.complexX(
                bitmapAtFocusX, WIDTH, published.centerX, published.scale);

        ViewportTransforms.State after = ViewportTransforms.commitFrozenGesture(
                published, accumulatedScale, positionX, positionY,
                focusX, focusY, WIDTH, HEIGHT);

        // After commit the screen shows the target viewport under identity transform.
        double complexAfterX = ViewportTransforms.complexX(
                focusX, WIDTH, after.centerX, after.scale);

        assertEquals(published.scale * accumulatedScale, after.scale, EPS);
        assertEquals(complexBeforeX, complexAfterX, 1e-6, "real under focus jumped");

        float bitmapAtFocusY = (focusY - positionY - (1f - accumulatedScale) * focusY)
                / accumulatedScale;
        double complexBeforeY = ViewportTransforms.complexY(
                bitmapAtFocusY, HEIGHT, published.centerY, published.scale);
        double complexAfterY = ViewportTransforms.complexY(
                focusY, HEIGHT, after.centerY, after.scale);
        assertEquals(complexBeforeY, complexAfterY, 1e-6, "imag under focus jumped");
    }

    @Test
    void frozenGesturePurePanReducesToCommitPan() {
        ViewportTransforms.State before = new ViewportTransforms.State(0.05, -0.15, 130.0);
        float panX = 45f;
        float panY = -70f;

        ViewportTransforms.State viaFrozen = ViewportTransforms.commitFrozenGesture(
                before, 1f, panX, panY, 200f, 800f, WIDTH, HEIGHT);
        ViewportTransforms.State viaPan = ViewportTransforms.commitPan(before, panX, panY);

        assertEquals(viaPan.centerX, viaFrozen.centerX, EPS);
        assertEquals(viaPan.centerY, viaFrozen.centerY, EPS);
        assertEquals(viaPan.scale, viaFrozen.scale, EPS);
    }

    @Test
    void frozenGestureCenteredPinchReducesToCenterPreservingZoom() {
        ViewportTransforms.State before = new ViewportTransforms.State(0.0, 0.0, 110.0);
        float s = 1.6f;

        ViewportTransforms.State after = ViewportTransforms.commitFrozenGesture(
                before, s, 0f, 0f, WIDTH / 2f, HEIGHT / 2f, WIDTH, HEIGHT);

        assertEquals(before.centerX, after.centerX, EPS);
        assertEquals(before.centerY, after.centerY, EPS);
        assertEquals(before.scale * s, after.scale, EPS);
    }
}
