package org.girino.frac.android.foss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.view.ViewGroup;
import android.widget.ListView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BottomSheetPickerHelperTest {

    @Test
    public void prepareList_capsHeightBelowFullScreen() {
        ListView list = new ListView(RuntimeEnvironment.getApplication());
        int screenHeight = list.getResources().getDisplayMetrics().heightPixels;
        BottomSheetPickerHelper.prepareList(list);
        ViewGroup.LayoutParams params = list.getLayoutParams();
        assertTrue(params.height > 0);
        assertTrue(params.height < screenHeight);
        assertEquals((int) (screenHeight * 0.55f), params.height);
    }
}
