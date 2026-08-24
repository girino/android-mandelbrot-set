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
    void incrementalComplexXStep_matchesDirectMapping() {
        int width = 1081;
        double scale = 123.456;
        double centerX = -0.75;
        for (int sampleStep : new int[] {1, 2, 4, 8}) {
            double cRe = ViewportTransforms.complexX(0, width, centerX, scale);
            double cReStep = sampleStep / scale;
            for (int x = 0; x < width; x += sampleStep) {
                assertEquals(
                        ViewportTransforms.complexX(x, width, centerX, scale),
                        cRe,
                        EPS);
                cRe += cReStep;
            }
        }
    }

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

    @Test
    void affinePreviewCommitMatchesIdentityAtEveryProbe() {
        ViewportTransforms.State before = new ViewportTransforms.State(-0.4, 0.15, 180.0);
        float s = 1.75f;
        float startFocusX = 820f;
        float startFocusY = 1500f;
        float focusX = 880f;
        float focusY = 1545f;
        float posX = 10f;
        float posY = -5f;
        float dx = posX + focusX - s * startFocusX;
        float dy = posY + focusY - s * startFocusY;

        ViewportTransforms.State after =
                ViewportTransforms.commitAffinePreview(before, s, dx, dy, WIDTH, HEIGHT);

        float[][] probes = {
            {100f, 300f}, {540f, 960f}, {900f, 1500f}, {focusX, focusY}
        };
        for (float[] q : probes) {
            float bitmapX = (q[0] - posX - focusX) / s + startFocusX;
            float bitmapY = (q[1] - posY - focusY) / s + startFocusY;
            double previewX =
                    ViewportTransforms.complexX(bitmapX, WIDTH, before.centerX, before.scale);
            double previewY =
                    ViewportTransforms.complexY(bitmapY, HEIGHT, before.centerY, before.scale);
            assertEquals(
                    previewX,
                    ViewportTransforms.complexX(q[0], WIDTH, after.centerX, after.scale),
                    1e-6);
            assertEquals(
                    previewY,
                    ViewportTransforms.complexY(q[1], HEIGHT, after.centerY, after.scale),
                    1e-6);
        }
    }
}
