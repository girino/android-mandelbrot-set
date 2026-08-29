package org.girino.frac.android.foss;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IterationSettingsSoftWarnTest {

    @Test
    public void fixedMode_ignoresAdaptiveCap() {
        assertFalse(IterationSettings.exceedsSoftWarn(
                IterationSettings.Mode.FIXED,
                512,
                IterationSettings.DEFAULT_BASE_MAX,
                IterationSettings.DEFAULT_ABSOLUTE_CAP));
    }

    @Test
    public void fixedMode_warnsWhenFixedAboveSoftCap() {
        assertTrue(IterationSettings.exceedsSoftWarn(
                IterationSettings.Mode.FIXED,
                IterationSettings.SOFT_ITER_WARN + 1,
                40,
                IterationSettings.DEFAULT_ABSOLUTE_CAP));
    }

    @Test
    public void adaptiveMode_warnsOnHighAbsoluteCap() {
        assertTrue(IterationSettings.exceedsSoftWarn(
                IterationSettings.Mode.ADAPTIVE,
                64,
                40,
                IterationSettings.SOFT_ITER_WARN + 1));
    }

    @Test
    public void scaleWithZoom_warnsOnBaseOnly() {
        assertTrue(IterationSettings.exceedsSoftWarn(
                IterationSettings.Mode.SCALE_WITH_ZOOM,
                64,
                IterationSettings.SOFT_ITER_WARN + 1,
                IterationSettings.DEFAULT_ABSOLUTE_CAP));
        assertFalse(IterationSettings.exceedsSoftWarn(
                IterationSettings.Mode.SCALE_WITH_ZOOM,
                IterationSettings.SOFT_ITER_WARN + 1,
                40,
                IterationSettings.DEFAULT_ABSOLUTE_CAP));
    }
}
