package org.girino.frac.android.foss;

import org.robolectric.shadows.ShadowLooper;

/** Tear-down helper for Robolectric tests that touch MandelbrotView render threads. */
final class MandelbrotViewTestHelper {

    private MandelbrotViewTestHelper() {
    }

    static void release(MandelbrotView view) {
        if (view == null) {
            return;
        }
        view.testingStopRender();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        view.testingReleaseBitmap();
    }
}
