package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Headless gesture tests for {@link MandelbrotView} (deferred-commit model). */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class MandelbrotViewGestureTest {

    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1920;
    private static final double EPS = 1e-4;

    private MandelbrotView view;

    @Before
    public void setUp() {
        view = new MandelbrotView(RuntimeEnvironment.getApplication());
        view.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(WIDTH, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(HEIGHT, android.view.View.MeasureSpec.EXACTLY));
        view.layout(0, 0, WIDTH, HEIGHT);
        view.testingStopRender();
    }

    /**
     * Pan commits a pending target on UP; the frozen preview stays on screen
     * (positionX/Y unchanged) until the render publishes — no premature clear.
     */
    @Test
    public void singleFingerPan_pendingTargetAndFrozenPreview() {
        double scale0 = view.testingScale();
        new PinchDragMotionSimulator().panOnly(view, 300f, 800f, 120f, -80f, 4);

        assertTrue(view.testingHasPendingTarget());
        assertEquals(-120f / scale0,
                view.testingTargetCenterX() - view.testingCenterX(), EPS);
        assertEquals(80f / scale0,
                view.testingTargetCenterY() - view.testingCenterY(), EPS);
        assertEquals(scale0, view.testingTargetScale(), EPS);
        // Published viewport and frozen preview untouched until bitmap publish.
        assertEquals(scale0, view.testingScale(), EPS);
        assertEquals(120f, view.testingPositionX(), 0f);
        assertEquals(-80f, view.testingPositionY(), 0f);
        assertEquals(1f, view.testingAccumulatedScale(), 0f);
    }

    /**
     * Premature onScaleEnd (first finger lifted) must NOT commit; only the last
     * ACTION_UP folds the accumulated zoom into the pending target.
     */
    @Test
    public void pinchZoom_commitOnlyWhenLastFingerLifts() {
        PinchDragMotionSimulator sim = new PinchDragMotionSimulator();
        float spanStart = 360f;
        float spanEnd = 520f;
        double scale0 = view.testingScale();

        sim.pinchDown(view, 520f, 1100f, spanStart);
        sim.pinchMove(view, 520f, 1100f, (spanStart + spanEnd) / 2f);
        assertFalse(view.testingHasPendingTarget());

        sim.pinchMoveThenOneFingerUp(view, 520f, 1100f, spanEnd);
        assertFalse(view.testingHasPendingTarget());

        sim.lastFingerUp(view, 520f, 1100f);
        assertTrue(view.testingHasPendingTarget());
        // Robolectric may skip the first onScale event; require a real zoom commit.
        assertTrue(view.testingTargetScale() > scale0 * 1.1);
        assertEquals(scale0, view.testingScale(), EPS);
    }

    @Test
    public void publishGate_blocksMidGestureAndStaleGenerations() {
        int current = view.testingRenderGeneration();

        assertFalse(view.testingWouldPublishBitmap(current - 1));
        assertFalse(view.testingWouldPublishBitmap(current + 1));

        view.testingSimulatePointersDown();
        assertFalse(view.testingWouldPublishBitmap(current));

        view.testingSimulateAllPointersUp();
        assertTrue(view.testingWouldPublishBitmap(current));
    }

    /** Pan then pinch: zoom accumulates through the second finger and commits on release. */
    @Test
    public void panThenPinch_zoomSurvivesSecondFingerDrop() {
        PinchDragMotionSimulator sim = new PinchDragMotionSimulator();
        double scale0 = view.testingScale();

        sim.panThenPinch(view, 400f, 900f, 80f, -40f, 4, 600f, 900f, 300f, 450f, 6);

        assertTrue(view.testingHasPendingTarget());
        assertTrue(view.testingTargetScale() > scale0 * 1.2);
        // Pan folded into the target as well.
        assertTrue(view.testingTargetCenterX() != view.testingCenterX()
                || view.testingTargetCenterY() != view.testingCenterY());
    }

    /**
     * Regression for issue #3: after the gesture, the pending target must show
     * exactly what the preview showed — same complex coordinate at EVERY screen
     * point, not just under the focus.
     */
    @Test
    public void offCenterPinch_previewAndCommitShowSameContent() {
        double scale0 = view.testingScale();
        float focusX = 820f;
        float focusY = 1500f;
        float spanStart = 300f;
        float spanEnd = 540f;

        PinchDragMotionSimulator sim = new PinchDragMotionSimulator();
        sim.pinchDown(view, focusX, focusY, spanStart);
        // Warm-up MOVE (no motion): guarantees the detector has begun with
        // startFocus exactly at (focusX, focusY) under Robolectric's legacy
        // detector, which only starts on the first MOVE.
        sim.pinchMove(view, focusX, focusY, spanStart);
        // Now real moves: scale grows and the focus drifts.
        sim.pinchMove(view, focusX + 30f, focusY + 20f, (spanStart + spanEnd) / 2f);
        sim.pinchMove(view, focusX + 60f, focusY + 45f, spanEnd);
        assertTrue(view.testingAccumulatedScale() != 1f);

        // Snapshot the live preview content at several screen points.
        float[][] probes = {
            {100f, 300f}, {540f, 960f}, {900f, 1500f}, {focusX + 60f, focusY + 45f}
        };
        double[] previewAt = new double[probes.length];
        double[] previewAtY = new double[probes.length];
        for (int i = 0; i < probes.length; i++) {
            previewAt[i] = view.testingPreviewComplexX(probes[i][0], probes[i][1]);
            previewAtY[i] = view.testingPreviewComplexY(probes[i][0], probes[i][1]);
        }

        sim.lastFingerUp(view, focusX + 60f, focusY + 45f);
        assertTrue(view.testingHasPendingTarget());

        // The committed viewport must display the identical content everywhere.
        for (int i = 0; i < probes.length; i++) {
            double targetCx = org.girino.frac.viewport.ViewportTransforms.complexX(
                    probes[i][0], WIDTH, view.testingTargetCenterX(), view.testingTargetScale());
            assertEquals(previewAt[i], targetCx, EPS * Math.max(1, scale0));
            double targetCy = org.girino.frac.viewport.ViewportTransforms.complexY(
                    probes[i][1], HEIGHT, view.testingTargetCenterY(), view.testingTargetScale());
            assertEquals(previewAtY[i], targetCy, EPS * Math.max(1, scale0));
        }
    }

    /**
     * Centered pinch keeps the exact v1.0.2 behavior: center preserved, only
     * scale changes in the pending target.
     */
    @Test
    public void centeredPinch_keepsCenterInPendingTarget() {
        double scale0 = view.testingScale();
        double centerX0 = view.testingCenterX();
        double centerY0 = view.testingCenterY();
        float midX = WIDTH / 2f;
        float midY = HEIGHT / 2f;

        PinchDragMotionSimulator sim = new PinchDragMotionSimulator();
        sim.pinchDown(view, midX, midY, 320f);
        // First MOVE only begins the detector under Robolectric; second produces onScale.
        sim.pinchMove(view, midX, midY, 400f);
        sim.pinchMove(view, midX, midY, 480f);
        sim.lastFingerUp(view, midX, midY);

        assertTrue(view.testingHasPendingTarget());
        assertTrue(view.testingTargetScale() > scale0);
        assertEquals(centerX0, view.testingTargetCenterX(), EPS);
        assertEquals(centerY0, view.testingTargetCenterY(), EPS);
    }

    /**
     * Zoom-in preview must track the moving focus (content walks with the
     * fingers): the complex coordinate under the focus stays the same while
     * the focus moves and the scale grows.
     */
    @Test
    public void zoomInPreviewTracksMovingFocus() {
        double scale0 = view.testingScale();
        double centerX0 = view.testingCenterX();
        float midX = 820f;
        float midY = HEIGHT / 2f;
        PinchDragMotionSimulator sim = new PinchDragMotionSimulator();
        sim.pinchDown(view, midX, midY, 300f);
        // Warm-up MOVE (no motion): starts the detector with startFocus at
        // (midX, midY) exactly, avoiding Robolectric's first-MOVE start quirk.
        sim.pinchMove(view, midX, midY, 300f);
        sim.pinchMove(view, midX + 40f, midY + 20f, 400f);
        double underFocusFirst = view.testingPreviewComplexX(midX + 40f, midY + 20f);

        sim.pinchMove(view, midX + 80f, midY + 40f, 540f);
        double underFocusSecond = view.testingPreviewComplexX(midX + 80f, midY + 40f);

        // Content follows the fingers: same complex under the moved focus.
        assertEquals(underFocusFirst, underFocusSecond, EPS * Math.max(1, scale0));
    }
}
