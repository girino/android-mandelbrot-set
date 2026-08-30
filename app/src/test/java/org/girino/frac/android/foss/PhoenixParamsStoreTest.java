package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class PhoenixParamsStoreTest {

    @Test
    public void saveAndLoad_roundTrip() {
        PhoenixParamsStore.save(
                org.robolectric.RuntimeEnvironment.getApplication(), -0.25, 0.0);
        PhoenixParamsStore.Params loaded =
                PhoenixParamsStore.load(org.robolectric.RuntimeEnvironment.getApplication());
        assertEquals(-0.25, loaded.pRe, 1e-12);
        assertEquals(0.0, loaded.pIm, 1e-12);
    }

    @Test
    public void load_defaultsWhenEmpty() {
        PhoenixParamsStore.Params loaded =
                PhoenixParamsStore.load(org.robolectric.RuntimeEnvironment.getApplication());
        assertEquals(PhoenixParamsStore.DEFAULT_P_RE, loaded.pRe, 1e-12);
        assertEquals(PhoenixParamsStore.DEFAULT_P_IM, loaded.pIm, 1e-12);
    }
}
