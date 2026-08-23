package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.os.Bundle;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Viewport save/restore across rotation (issue #21). */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class ViewportSessionTest {

    private static final int PORTRAIT_W = 320;
    private static final int PORTRAIT_H = 480;
    private static final int LANDSCAPE_W = 480;
    private static final int LANDSCAPE_H = 320;
    private static final double EPS = 1e-6;

    private MandelbrotView view;
    private MandelbrotView rotated;

    @After
    public void tearDown() {
        if (view != null) {
            view.testingReleaseBitmap();
            view = null;
        }
        if (rotated != null) {
            rotated.testingReleaseBitmap();
            rotated = null;
        }
    }

    @Test
    public void bundleRoundTrip_preservesFields() {
        view = laidOutView(PORTRAIT_W, PORTRAIT_H);
        view.zoomIn();
        view.setOper(FormulaCatalog.get(1));
        view.setPalette(PaletteCatalog.get(2));
        view.smooth();

        ViewportSession original = view.captureSession();
        Bundle bundle = original.toBundle();
        ViewportSession restored = ViewportSession.fromBundle(bundle);

        assertNotNull(restored);
        assertEquals(original.centerX, restored.centerX, EPS);
        assertEquals(original.centerY, restored.centerY, EPS);
        assertEquals(original.scale, restored.scale, EPS);
        assertEquals(original.viewWidth, restored.viewWidth);
        assertEquals(original.viewHeight, restored.viewHeight);
        assertEquals(original.operatorIndex, restored.operatorIndex);
        assertEquals(original.paletteIndex, restored.paletteIndex);
        assertEquals(original.smooth, restored.smooth);
    }

    @Test
    public void restoreSession_keepsComplexCenterThroughResize() {
        view = laidOutView(PORTRAIT_W, PORTRAIT_H);
        view.setOper(FormulaCatalog.get(1));
        view.setPalette(PaletteCatalog.get(2));
        view.smooth();
        view.zoomIn();
        view.zoomIn();
        double centerX = view.testingCenterX();
        double centerY = view.testingCenterY();
        ViewportSession session = view.captureSession();

        rotated = new MandelbrotView(RuntimeEnvironment.getApplication());
        rotated.restoreSession(session);
        layout(rotated, LANDSCAPE_W, LANDSCAPE_H);
        rotated.testingStopRender();

        assertEquals(centerX, rotated.testingCenterX(), EPS);
        assertEquals(centerY, rotated.testingCenterY(), EPS);
        ViewportSession after = rotated.captureSession();
        assertEquals(session.operatorIndex, after.operatorIndex);
        assertEquals(session.paletteIndex, after.paletteIndex);
        assertEquals(session.smooth, after.smooth);
    }

    private static MandelbrotView laidOutView(int width, int height) {
        MandelbrotView target = new MandelbrotView(RuntimeEnvironment.getApplication());
        layout(target, width, height);
        target.testingStopRender();
        return target;
    }

    private static void layout(MandelbrotView target, int width, int height) {
        target.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY));
        target.layout(0, 0, width, height);
    }
}
