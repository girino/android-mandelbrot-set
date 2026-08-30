package org.girino.frac.android.foss;

import android.view.ViewGroup;
import android.widget.ListView;

/** Scrollable ListView inside Material bottom sheets (long formula/palette lists). */
final class BottomSheetPickerHelper {

    /** Visible list area as a fraction of screen height — leaves room for title + sheet chrome. */
    private static final float LIST_HEIGHT_FRACTION = 0.55f;

    private BottomSheetPickerHelper() {
    }

    static void prepareList(ListView list) {
        int maxHeight = (int) (list.getResources().getDisplayMetrics().heightPixels * LIST_HEIGHT_FRACTION);
        ViewGroup.LayoutParams params = list.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, maxHeight);
        } else {
            params.height = maxHeight;
        }
        list.setLayoutParams(params);
        list.setNestedScrollingEnabled(true);
    }

    static void bindCheckedSelection(ListView list, int position, int itemCount) {
        if (itemCount <= 0) {
            return;
        }
        int safe = Math.max(0, Math.min(position, itemCount - 1));
        list.setItemChecked(safe, true);
        list.post(() -> {
            int offset = Math.max(list.getHeight() / 4, 0);
            list.setSelectionFromTop(safe, offset);
        });
    }
}
