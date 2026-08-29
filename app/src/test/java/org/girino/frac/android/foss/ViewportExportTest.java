package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Viewport PNG export (issue #18). */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class ViewportExportTest {

    private static final int WIDTH = 320;
    private static final int HEIGHT = 480;

    private MandelbrotView view;

    @After
    public void tearDown() {
        MandelbrotViewTestHelper.release(view);
        view = null;
    }

    @Test
    public void captureDisplayedViewport_matchesViewSize() {
        view = laidOutView(WIDTH, HEIGHT);
        Bitmap bitmap = view.captureDisplayedViewport();
        assertNotNull(bitmap);
        assertEquals(WIDTH, bitmap.getWidth());
        assertEquals(HEIGHT, bitmap.getHeight());
        bitmap.recycle();
    }

    @Test
    public void captureDisplayedViewport_returnsNullWhenUnlaidOut() {
        view = new MandelbrotView(RuntimeEnvironment.getApplication());
        assertEquals(null, view.captureDisplayedViewport());
    }

    @Test
    public void defaultFileName_usesPngSuffix() {
        String name = ViewportPngExporter.defaultFileName();
        assertTrue(name.startsWith("mandelbrot_"));
        assertTrue(name.endsWith(".png"));
    }

    @Test
    public void saveToGallery_returnsFalseBeforeApi29() {
        Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        assertFalse(ViewportPngExporter.saveToGallery(RuntimeEnvironment.getApplication(), bitmap));
        bitmap.recycle();
    }

    private static MandelbrotView laidOutView(int width, int height) {
        MandelbrotView target = new MandelbrotView(RuntimeEnvironment.getApplication());
        target.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY));
        target.layout(0, 0, width, height);
        target.testingStopRender();
        return target;
    }
}
