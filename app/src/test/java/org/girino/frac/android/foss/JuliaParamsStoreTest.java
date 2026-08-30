package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class JuliaParamsStoreTest {

    @Test
    public void saveAndLoad_roundTrip() {
        JuliaParamsStore.save(
                org.robolectric.RuntimeEnvironment.getApplication(), -0.4, 0.6);
        JuliaParamsStore.Params loaded =
                JuliaParamsStore.load(org.robolectric.RuntimeEnvironment.getApplication());
        assertEquals(-0.4, loaded.cRe, 1e-12);
        assertEquals(0.6, loaded.cIm, 1e-12);
    }
}
