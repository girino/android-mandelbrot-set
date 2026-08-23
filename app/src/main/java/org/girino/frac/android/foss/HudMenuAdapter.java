package org.girino.frac.android.foss;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

/** Hamburger menu rows: icon + label (issue #29). */
final class HudMenuAdapter extends BaseAdapter {

    private static final int TYPE_ACTION = 0;
    private static final int TYPE_SECTION = 1;

    /** Core HUD actions in canonical order (issue #29). */
    enum Action {
        ZOOM_IN(R.string.menu_zoom_in, R.drawable.ic_hud_zoom_in, R.string.hud_zoom_in_cd, false),
        ZOOM_OUT(R.string.menu_zoom_out, R.drawable.ic_hud_zoom_out, R.string.hud_zoom_out_cd, false),
        RESET(R.string.menu_reset, R.drawable.ic_hud_reset, R.string.hud_reset_cd, false),
        SMOOTH(R.string.menu_smooth_palette, R.drawable.ic_hud_smooth, R.string.hud_smooth_cd, true),
        FORMULA(R.string.menu_formula, R.drawable.ic_hud_formula, R.string.hud_formula_cd, false),
        PALETTE(R.string.menu_palette, R.drawable.ic_hud_palette, R.string.hud_palette_cd, false);

        final int labelRes;
        final int iconRes;
        final int contentDescriptionRes;
        final boolean toggleIndicator;

        Action(int labelRes, int iconRes, int contentDescriptionRes, boolean toggleIndicator) {
            this.labelRes = labelRes;
            this.iconRes = iconRes;
            this.contentDescriptionRes = contentDescriptionRes;
            this.toggleIndicator = toggleIndicator;
        }
    }

    private enum Overflow {
        EXPORT(R.string.menu_export, R.drawable.ic_hud_export, R.string.hud_export_cd),
        HELP(R.string.menu_help, R.drawable.ic_hud_help, R.string.hud_help_cd),
        ABOUT(R.string.menu_about, R.drawable.ic_hud_about, R.string.hud_about_cd);

        final int labelRes;
        final int iconRes;
        final int contentDescriptionRes;

        Overflow(int labelRes, int iconRes, int contentDescriptionRes) {
            this.labelRes = labelRes;
            this.iconRes = iconRes;
            this.contentDescriptionRes = contentDescriptionRes;
        }
    }

    private static final int CORE_COUNT = Action.values().length;
    private static final int SECTION_INDEX = CORE_COUNT;
    private static final int OVERFLOW_COUNT = Overflow.values().length;
    private static final int ITEM_COUNT = CORE_COUNT + 1 + OVERFLOW_COUNT;

    private final LayoutInflater inflater;
    private final Context context;
    private boolean smoothOn;

    HudMenuAdapter(Context context, boolean smoothOn) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.smoothOn = smoothOn;
    }

    void setSmoothOn(boolean smoothOn) {
        this.smoothOn = smoothOn;
        notifyDataSetChanged();
    }

    static boolean isSectionHeader(int position) {
        return position == SECTION_INDEX;
    }

    static boolean isExport(int position) {
        return overflowAt(position) == Overflow.EXPORT;
    }

    static boolean isHelp(int position) {
        return overflowAt(position) == Overflow.HELP;
    }

    static boolean isAbout(int position) {
        return overflowAt(position) == Overflow.ABOUT;
    }

    static Action actionAt(int position) {
        if (position < 0 || position >= CORE_COUNT) {
            return null;
        }
        return Action.values()[position];
    }

    private static Overflow overflowAt(int position) {
        int overflowIndex = position - SECTION_INDEX - 1;
        if (overflowIndex < 0 || overflowIndex >= OVERFLOW_COUNT) {
            return null;
        }
        return Overflow.values()[overflowIndex];
    }

    @Override
    public int getCount() {
        return ITEM_COUNT;
    }

    @Override
    public Object getItem(int position) {
        Action action = actionAt(position);
        if (action != null) {
            return action;
        }
        Overflow overflow = overflowAt(position);
        return overflow != null ? overflow : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return isSectionHeader(position) ? TYPE_SECTION : TYPE_ACTION;
    }

    @Override
    public boolean isEnabled(int position) {
        return !isSectionHeader(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == TYPE_SECTION) {
            TextView header = convertView instanceof TextView
                    ? (TextView) convertView
                    : (TextView) inflater.inflate(R.layout.hud_menu_section, parent, false);
            header.setText(R.string.hud_menu_more);
            return header;
        }

        View row = convertView;
        if (!(row instanceof ViewGroup)) {
            row = inflater.inflate(R.layout.hud_menu_item, parent, false);
        }
        ImageView icon = row.findViewById(R.id.hud_menu_icon);
        TextView label = row.findViewById(R.id.hud_menu_label);
        TextView indicator = row.findViewById(R.id.hud_menu_indicator);

        int labelRes;
        int iconRes;
        int contentDescriptionRes;
        boolean toggleIndicator;
        Action action = actionAt(position);
        if (action != null) {
            labelRes = action.labelRes;
            iconRes = action.iconRes;
            contentDescriptionRes = action.contentDescriptionRes;
            toggleIndicator = action.toggleIndicator;
        } else {
            Overflow overflow = overflowAt(position);
            labelRes = overflow.labelRes;
            iconRes = overflow.iconRes;
            contentDescriptionRes = overflow.contentDescriptionRes;
            toggleIndicator = false;
        }

        label.setText(labelRes);
        icon.setImageResource(iconRes);
        ImageViewCompat.setImageTintList(
                icon,
                ContextCompat.getColorStateList(context, R.color.hud_icon_tint));
        row.setContentDescription(context.getString(contentDescriptionRes));

        if (toggleIndicator) {
            indicator.setVisibility(View.VISIBLE);
            indicator.setText(smoothOn ? R.string.status_overlay_smooth_on : R.string.status_overlay_smooth_off);
        } else {
            indicator.setVisibility(View.GONE);
        }
        return row;
    }
}
