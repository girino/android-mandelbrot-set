package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Viewport PNG export (issue #18). */
@RunWith(RobolectricTestRunner.class)
public class ViewportExportTest {

    private static final int WIDTH = 320;
    private static final int HEIGHT = 480;

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

    @Test
    public void captureDisplayedViewport_matchesViewSize() {
        Bitmap bitmap = view.captureDisplayedViewport();
        assertNotNull(bitmap);
        assertEquals(WIDTH, bitmap.getWidth());
        assertEquals(HEIGHT, bitmap.getHeight());
        bitmap.recycle();
    }

    @Test
    public void captureDisplayedViewport_returnsNullWhenUnlaidOut() {
        MandelbrotView empty = new MandelbrotView(RuntimeEnvironment.getApplication());
        assertEquals(null, empty.captureDisplayedViewport());
    }

    @Test
    public void defaultFileName_usesPngSuffix() {
        String name = ViewportPngExporter.defaultFileName();
        assertTrue(name.startsWith("mandelbrot_"));
        assertTrue(name.endsWith(".png"));
    }

    @Test
    @Config(sdk = 28)
    public void saveToGallery_returnsFalseBeforeApi29() {
        Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        assertFalse(ViewportPngExporter.saveToGallery(RuntimeEnvironment.getApplication(), bitmap));
        bitmap.recycle();
    }
}
