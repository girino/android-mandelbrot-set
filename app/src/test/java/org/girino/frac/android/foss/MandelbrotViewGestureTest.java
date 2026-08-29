package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.atomic.AtomicBoolean;

/** Headless gesture tests for {@link MandelbrotView} (deferred-commit model). */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class MandelbrotViewGestureTest {

    private static final int WIDTH = 64;
    private static final int HEIGHT = 64;
    private static final double EPS = 1e-4;

    private MandelbrotView view;

    @Before
    public void setUp() {
        view = new MandelbrotView(RuntimeEnvironment.getApplication());
        view.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(WIDTH, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(HEIGHT, android.view.View.MeasureSpec.EXACTLY));
        view.layout(0, 0, WIDTH, HEIGHT);
        MandelbrotViewTestHelper.useFastFixedIterations(view);
        view.testingStopRender();
    }

    @After
    public void tearDown() {
        MandelbrotViewTestHelper.release(view);
        view = null;
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
     * Issue #3: pending target must show exactly what the preview showed —
     * same complex at every probed screen point (gaps during preview OK;
     * new bitmap fills the window after handoff).
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
        // Warm-up MOVE: Robolectric detector begins on first MOVE.
        sim.pinchMove(view, focusX, focusY, spanStart);
        sim.pinchMove(view, focusX + 30f, focusY + 20f, (spanStart + spanEnd) / 2f);
        sim.pinchMove(view, focusX + 60f, focusY + 45f, spanEnd);
        assertTrue(view.testingAccumulatedScale() != 1f);

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

        for (int i = 0; i < probes.length; i++) {
            double targetCx = org.girino.frac.viewport.ViewportTransforms.complexX(
                    probes[i][0], WIDTH, view.testingTargetCenterX(), view.testingTargetScale());
            assertEquals(previewAt[i], targetCx, EPS * Math.max(1, scale0));
            double targetCy = org.girino.frac.viewport.ViewportTransforms.complexY(
                    probes[i][1], HEIGHT, view.testingTargetCenterY(), view.testingTargetScale());
            assertEquals(previewAtY[i], targetCy, EPS * Math.max(1, scale0));
        }
    }

    /** Centered pinch keeps center; only scale changes (v1.0.2 special case). */
    @Test
    public void centeredPinch_keepsCenterInPendingTarget() {
        double scale0 = view.testingScale();
        double centerX0 = view.testingCenterX();
        double centerY0 = view.testingCenterY();
        float midX = WIDTH / 2f;
        float midY = HEIGHT / 2f;

        PinchDragMotionSimulator sim = new PinchDragMotionSimulator();
        sim.pinchDown(view, midX, midY, 320f);
        sim.pinchMove(view, midX, midY, 320f);
        sim.pinchMove(view, midX, midY, 400f);
        sim.pinchMove(view, midX, midY, 480f);
        sim.lastFingerUp(view, midX, midY);

        assertTrue(view.testingHasPendingTarget());
        assertTrue(view.testingTargetScale() > scale0);
        assertEquals(centerX0, view.testingTargetCenterX(), EPS);
        assertEquals(centerY0, view.testingTargetCenterY(), EPS);
    }

    /** Zoom-in preview tracks the moving focus (content walks with fingers). */
    @Test
    public void zoomInPreviewTracksMovingFocus() {
        double scale0 = view.testingScale();
        float midX = 820f;
        float midY = HEIGHT / 2f;
        PinchDragMotionSimulator sim = new PinchDragMotionSimulator();
        sim.pinchDown(view, midX, midY, 300f);
        sim.pinchMove(view, midX, midY, 300f);
        sim.pinchMove(view, midX + 40f, midY + 20f, 400f);
        double underFocusFirst = view.testingPreviewComplexX(midX + 40f, midY + 20f);

        sim.pinchMove(view, midX + 80f, midY + 40f, 540f);
        double underFocusSecond = view.testingPreviewComplexX(midX + 80f, midY + 40f);

        assertEquals(underFocusFirst, underFocusSecond, EPS * Math.max(1, scale0));
    }

    /**
     * Regression: atomic handoff must clear focus back to screen center.
     * Otherwise with s=1 the preview is q = p + (focus - startFocus) and the
     * new bitmap (which already baked in the drag) is translated again (~2x).
     */
    @Test
    public void atomicHandoff_resetsFocusToCenter() {
        float midX = 820f;
        float midY = 1500f;
        PinchDragMotionSimulator sim = new PinchDragMotionSimulator();
        sim.pinchDown(view, midX, midY, 300f);
        sim.pinchMove(view, midX, midY, 300f);
        sim.pinchMove(view, midX + 60f, midY + 45f, 540f);
        sim.lastFingerUp(view, midX + 60f, midY + 45f);

        view.testingSimulateAtomicHandoffClear();
        assertEquals(WIDTH / 2f, view.testingFocusX(), 0f);
        assertEquals(HEIGHT / 2f, view.testingFocusY(), 0f);
        assertEquals(view.testingFocusX(), view.testingStartFocusX(), 0f);
        assertEquals(view.testingFocusY(), view.testingStartFocusY(), 0f);
        assertEquals(0f, view.testingPositionX(), 0f);
        assertEquals(1f, view.testingAccumulatedScale(), 0f);
    }

    /** Issue #4: Smooth palette must flip the smooth flag (not only re-render). */
    @Test
    public void smoothToggle_flipsFlagEachCall() {
        assertFalse(view.isSmooth());
        view.smooth();
        assertTrue(view.isSmooth());
        view.smooth();
        assertFalse(view.isSmooth());
    }

    /** Issue #6: centered zoom in/out changes scale and keeps center. */
    @Test
    public void zoomInOut_centeredKeepCenter() {
        double scale0 = view.testingScale();
        double cx = view.testingCenterX();
        double cy = view.testingCenterY();

        view.zoomIn();
        assertTrue(view.testingHasPendingTarget());
        assertEquals(scale0 * 1.5, view.testingTargetScale(), EPS);
        assertEquals(cx, view.testingTargetCenterX(), EPS);
        assertEquals(cy, view.testingTargetCenterY(), EPS);

        // Simulate publish so the next zoom starts from the new scale.
        view.testingApplyPendingAsPublished();
        double scale1 = view.testingScale();
        view.zoomOut();
        assertEquals(scale1 / 1.5, view.testingTargetScale(), EPS);
        assertEquals(cx, view.testingTargetCenterX(), EPS);
        assertEquals(cy, view.testingTargetCenterY(), EPS);
    }

    /** Issue #6: zoomAt keeps the complex point under the focus fixed. */
    @Test
    public void zoomAt_keepsComplexUnderFocus() {
        double scale0 = view.testingScale();
        double cx = view.testingCenterX();
        float focusX = 820f;
        float focusY = 1500f;
        double underBefore = org.girino.frac.viewport.ViewportTransforms.complexX(
                focusX, WIDTH, cx, scale0);

        view.zoomAt(focusX, focusY, 1.5);
        assertTrue(view.testingHasPendingTarget());
        double underAfter = org.girino.frac.viewport.ViewportTransforms.complexX(
                focusX, WIDTH, view.testingTargetCenterX(), view.testingTargetScale());
        assertEquals(underBefore, underAfter, EPS * Math.max(1, scale0));
        assertTrue(view.testingTargetScale() > scale0);
    }

    /**
     * Issue #6: double-tap must start a render even though the second tap's
     * finger is still down when onDoubleTap fires (activePointers gate).
     */
    @Test
    public void doubleTap_queuesPendingTargetDespiteFingerDown() {
        double scale0 = view.testingScale();
        // Simulate a second-tap DOWN that leaves activePointers at 1, then
        // the same zoomAt path the GestureDetector uses after clearing pointers.
        view.testingSimulatePointersDown();
        assertEquals(1, view.testingActivePointers());
        view.testingSimulateDoubleTapZoom(820f, 1500f);
        assertEquals(0, view.testingActivePointers());
        assertTrue(view.testingHasPendingTarget());
        assertTrue(view.testingTargetScale() > scale0);
    }

    /** Issue #11: complexAt matches published viewport at screen center. */
    @Test
    public void complexAt_screenCenter_matchesPublishedCenter() {
        assertEquals(view.testingCenterX(), view.complexRealAt(WIDTH / 2f, HEIGHT / 2f), EPS);
        assertEquals(view.testingCenterY(), view.complexImagAt(WIDTH / 2f, HEIGHT / 2f), EPS);
    }

    /** Issue #9: start marks busy; stop clears it. */
    @Test
    public void start_setsRenderBusy_stop_clears() {
        assertFalse(view.testingRenderBusy());
        view.start();
        assertTrue(view.testingRenderBusy());
        view.stop();
        view.testingAwaitRenderIdle(3000L);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertFalse(view.testingRenderBusy());
    }

    /** Issue #9: start gated by active pointers must not claim busy. */
    @Test
    public void start_withPointersDown_staysNotBusy() {
        view.testingSimulatePointersDown();
        view.start();
        assertFalse(view.testingRenderBusy());
    }

    /**
     * Regression: second pinch while the first zoom's frozen preview is still
     * on screen (render 8→4→2→1 in flight) must not reset focus to center on
     * ACTION_DOWN — that breaks the pinch anchor and throws content off-screen.
     */
    @Test
    public void secondPinchDuringFrozenPreview_preservesAnchorAndCommit() {
        double scale0 = view.testingScale();
        float focusX = 48f;
        float focusY = 40f;
        float spanStart = 300f;
        float spanEnd = 540f;

        PinchDragMotionSimulator sim = new PinchDragMotionSimulator();
        sim.pinchDown(view, focusX, focusY, spanStart);
        sim.pinchMove(view, focusX, focusY, spanStart);
        sim.pinchMove(view, focusX + 4f, focusY + 3f, (spanStart + spanEnd) / 2f);
        sim.pinchMove(view, focusX + 6f, focusY + 4f, spanEnd);
        assertTrue(view.testingAccumulatedScale() > 1.05f);
        sim.lastFingerUp(view, focusX + 6f, focusY + 4f);

        assertTrue(view.testingHasPendingTarget());
        float focusAfterFirst = view.testingFocusX();
        assertTrue(Math.abs(focusAfterFirst - WIDTH / 2f) > 1f);

        view.start();

        sim.pinchDown(view, focusX + 6f, focusY + 4f, spanStart);
        assertEquals(focusAfterFirst, view.testingFocusX(), 1f);

        sim.pinchMove(view, focusX + 6f, focusY + 4f, spanStart);
        sim.pinchMove(view, focusX + 10f, focusY + 7f, spanEnd);

        float[][] probes = {
            {16f, 20f}, {WIDTH / 2f, HEIGHT / 2f}, {52f, 44f}, {focusX + 10f, focusY + 7f}
        };
        double[] previewCx = new double[probes.length];
        double[] previewCy = new double[probes.length];
        for (int i = 0; i < probes.length; i++) {
            previewCx[i] = view.testingPreviewComplexX(probes[i][0], probes[i][1]);
            previewCy[i] = view.testingPreviewComplexY(probes[i][0], probes[i][1]);
        }

        sim.lastFingerUp(view, focusX + 10f, focusY + 7f);
        assertTrue(view.testingHasPendingTarget());
        assertTrue(view.testingTargetScale() > scale0 * 1.2);

        for (int i = 0; i < probes.length; i++) {
            double targetCx = org.girino.frac.viewport.ViewportTransforms.complexX(
                    probes[i][0], WIDTH, view.testingTargetCenterX(), view.testingTargetScale());
            assertEquals(previewCx[i], targetCx, EPS * Math.max(1, scale0));
            double targetCy = org.girino.frac.viewport.ViewportTransforms.complexY(
                    probes[i][1], HEIGHT, view.testingTargetCenterY(), view.testingTargetScale());
            assertEquals(previewCy[i], targetCy, EPS * Math.max(1, scale0));
        }
    }

    /** Issue #48: skip Adaptive progressive publish when pass found no escapes. */
    @Test
    public void skipAdaptiveProgressivePublish_onlyWhenAdaptiveAndAllInterior() {
        AtomicBoolean escapes = new AtomicBoolean(false);
        assertTrue(MandelbrotView.skipAdaptiveProgressivePublish(true, escapes));
        escapes.set(true);
        assertFalse(MandelbrotView.skipAdaptiveProgressivePublish(true, escapes));
        assertFalse(MandelbrotView.skipAdaptiveProgressivePublish(false, escapes));
        assertFalse(MandelbrotView.skipAdaptiveProgressivePublish(true, null));
    }

    /** Issue #48: skip Adaptive UI publish until first colored escape. */
    @Test
    public void skipAdaptiveUiPublish_untilColoredEscapeSeen() {
        AtomicBoolean seen = new AtomicBoolean(false);
        assertTrue(MandelbrotView.skipAdaptiveUiPublish(seen));
        seen.set(true);
        assertFalse(MandelbrotView.skipAdaptiveUiPublish(seen));
        assertFalse(MandelbrotView.skipAdaptiveUiPublish(null));
    }

    /** Issue #9: sample count matches progressive 8→4→2→1 grid visits. */
    @Test
    public void progressiveSampleCount_matchesNestedLoops() {
        int w = 1080;
        int h = 1920;
        int expected = 0;
        for (int step = 8; step > 0; step /= 2) {
            for (int y = 0; y < h; y += step) {
                for (int x = 0; x < w; x += step) {
                    expected++;
                }
            }
        }
        assertEquals(expected, MandelbrotView.progressiveSampleCount(w, h));
        assertEquals(0, MandelbrotView.progressiveSampleCount(0, h));
    }
}
