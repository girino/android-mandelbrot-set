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
     * Regression for issue #3: the complex point under the pinch focus must be
     * identical before the gesture (published viewport), during the preview
     * (same point under the canvas transform) and after commit (pending target).
     */
    @Test
    public void offCenterPinch_focusPointStableThroughPreviewAndCommit() {
        double scale0 = view.testingScale();
        double centerX0 = view.testingCenterX();
        float focusX = 820f;
        float focusY = 1500f;
        float spanStart = 300f;
        float spanEnd = 540f;

        // Complex point under the focus before any gesture.
        double underFocusBefore = org.girino.frac.viewport.ViewportTransforms.complexX(
                focusX, WIDTH, centerX0, scale0);

        PinchDragMotionSimulator sim = new PinchDragMotionSimulator();
        sim.pinchDown(view, focusX, focusY, spanStart);
        // First MOVE only begins the detector under Robolectric; second produces onScale.
        sim.pinchMove(view, focusX, focusY, (spanStart + spanEnd) / 2f);
        sim.pinchMove(view, focusX, focusY, spanEnd);

        // During preview: screen(focus) shows bitmap p with q = s*p + pos + (1-s)*focus.
        float accScale = view.testingAccumulatedScale();
        assertTrue(accScale > 1.05);
        float bitmapAtFocus =
                (focusX - view.testingPositionX() - (1f - accScale) * focusX) / accScale;
        double underFocusPreview = org.girino.frac.viewport.ViewportTransforms.complexX(
                bitmapAtFocus, WIDTH, centerX0, scale0);
        assertEquals(underFocusBefore, underFocusPreview, EPS * Math.max(1, scale0));

        sim.lastFingerUp(view, focusX, focusY);

        // After commit: pending target keeps the same complex point under the focus,
        // now rendered under identity transform.
        assertTrue(view.testingHasPendingTarget());
        double underFocusAfter = org.girino.frac.viewport.ViewportTransforms.complexX(
                focusX, WIDTH, view.testingTargetCenterX(), view.testingTargetScale());
        assertEquals(underFocusBefore, underFocusAfter, EPS * Math.max(1, scale0));
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
}
