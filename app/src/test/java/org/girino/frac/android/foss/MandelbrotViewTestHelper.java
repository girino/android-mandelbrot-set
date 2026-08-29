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
        view.testingAwaitRenderIdle(3000L);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        view.testingReleaseBitmap();
    }

    /** Low iteration cap so accidental renders stay fast and deterministic. */
    static void useFastFixedIterations(MandelbrotView view) {
        view.testingSetIterationSettings(new IterationSettings(
                IterationSettings.Mode.FIXED,
                12,
                12,
                1.2,
                1,
                64));
    }
}
