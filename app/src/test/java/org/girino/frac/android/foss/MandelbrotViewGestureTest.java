package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.view.View;

import org.girino.frac.viewport.ViewportTransforms;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Headless gesture tests for the v1.0.0-style {@link MandelbrotView}. */
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
                View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, WIDTH, HEIGHT);
        view.testingStopRender();
    }

    @Test
    public void singleFingerPan_commitsOnUp() {
        ViewportTransforms.State start =
                new ViewportTransforms.State(
                        view.testingCenterX(), view.testingCenterY(), view.testingScale());
        new PinchDragMotionSimulator().panOnly(view, 300f, 800f, 120f, -80f, 4);
        ViewportTransforms.State expected = ViewportTransforms.commitPan(start, 120f, -80f);
        assertEquals(expected.centerX, view.testingCenterX(), EPS);
        assertEquals(expected.centerY, view.testingCenterY(), EPS);
    }

    @Test
    public void scaleDetectorPinch_increasesScale() {
        double startScale = view.testingScale();
        new PinchDragMotionSimulator()
                .pinchDrag(view, 520f, 1100f, 360f, 520f, 0f, 0f, 8);
        assertTrue(view.testingScale() > startScale * 1.2);
        assertEquals(1f, view.testingAccumulatedScale(), 0f);
    }
}
