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

/** Hamburger menu rows: icon + label (issues #29 / #46). */
final class HudMenuAdapter extends BaseAdapter {

    private static final int TYPE_ACTION = 0;
    private static final int TYPE_SECTION = 1;

    enum Action {
        ZOOM_IN(R.string.menu_zoom_in, R.drawable.ic_hud_zoom_in, R.string.hud_zoom_in_cd, false),
        ZOOM_OUT(R.string.menu_zoom_out, R.drawable.ic_hud_zoom_out, R.string.hud_zoom_out_cd, false),
        RESET(R.string.menu_reset, R.drawable.ic_hud_reset, R.string.hud_reset_cd, false),
        FORMULA(R.string.menu_formula, R.drawable.ic_hud_formula, R.string.hud_formula_cd, false),
        PALETTE(R.string.menu_palette, R.drawable.ic_hud_palette, R.string.hud_palette_cd, false);

        final int labelRes;
        final int iconRes;
        final int contentDescriptionRes;
        final boolean valueIndicator;

        Action(int labelRes, int iconRes, int contentDescriptionRes, boolean valueIndicator) {
            this.labelRes = labelRes;
            this.iconRes = iconRes;
            this.contentDescriptionRes = contentDescriptionRes;
            this.valueIndicator = valueIndicator;
        }
    }

    private enum Overflow {
        SMOOTH(R.string.menu_smooth_palette, R.drawable.ic_hud_smooth, R.string.hud_smooth_cd, true),
        ITERATIONS(R.string.menu_iterations, R.drawable.ic_hud_iterations, R.string.hud_iterations_cd, false),
        PHOENIX_PARAMS(
                R.string.menu_phoenix_params,
                R.drawable.ic_hud_phoenix_params,
                R.string.hud_phoenix_params_cd,
                true),
        JULIA_PARAMS(
                R.string.menu_julia_params,
                R.drawable.ic_hud_julia_params,
                R.string.hud_julia_params_cd,
                true),
        HELP(R.string.menu_help, R.drawable.ic_hud_help, R.string.hud_help_cd, false),
        ABOUT(R.string.menu_about, R.drawable.ic_hud_about, R.string.hud_about_cd, false);

        final int labelRes;
        final int iconRes;
        final int contentDescriptionRes;
        final boolean valueIndicator;

        Overflow(int labelRes, int iconRes, int contentDescriptionRes, boolean valueIndicator) {
            this.labelRes = labelRes;
            this.iconRes = iconRes;
            this.contentDescriptionRes = contentDescriptionRes;
            this.valueIndicator = valueIndicator;
        }
    }

    private static final int CORE_COUNT = Action.values().length;
    private static final int SECTION_INDEX = CORE_COUNT;

    private final LayoutInflater inflater;
    private final Context context;
    private final boolean smoothOn;
    private final boolean phoenixParamsVisible;
    private final String phoenixParamsIndicator;
    private final boolean juliaParamsVisible;
    private final String juliaParamsIndicator;

    HudMenuAdapter(
            Context context,
            boolean smoothOn,
            boolean phoenixParamsVisible,
            String phoenixParamsIndicator,
            boolean juliaParamsVisible,
            String juliaParamsIndicator) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.smoothOn = smoothOn;
        this.phoenixParamsVisible = phoenixParamsVisible;
        this.phoenixParamsIndicator = phoenixParamsIndicator != null ? phoenixParamsIndicator : "";
        this.juliaParamsVisible = juliaParamsVisible;
        this.juliaParamsIndicator = juliaParamsIndicator != null ? juliaParamsIndicator : "";
    }

    boolean isSectionHeader(int position) {
        return position == SECTION_INDEX;
    }

    boolean isSmooth(int position) {
        return overflowAt(position) == Overflow.SMOOTH;
    }

    boolean isIterations(int position) {
        return overflowAt(position) == Overflow.ITERATIONS;
    }

    boolean isPhoenixParams(int position) {
        return overflowAt(position) == Overflow.PHOENIX_PARAMS;
    }

    boolean isJuliaParams(int position) {
        return overflowAt(position) == Overflow.JULIA_PARAMS;
    }

    boolean isHelp(int position) {
        return overflowAt(position) == Overflow.HELP;
    }

    boolean isAbout(int position) {
        return overflowAt(position) == Overflow.ABOUT;
    }

    static Action actionAt(int position) {
        if (position < 0 || position >= CORE_COUNT) {
            return null;
        }
        return Action.values()[position];
    }

    private boolean isOverflowVisible(Overflow item) {
        if (item == Overflow.PHOENIX_PARAMS) {
            return phoenixParamsVisible;
        }
        if (item == Overflow.JULIA_PARAMS) {
            return juliaParamsVisible;
        }
        return true;
    }

    private Overflow overflowAt(int position) {
        int overflowIndex = position - SECTION_INDEX - 1;
        if (overflowIndex < 0) {
            return null;
        }
        int slot = 0;
        for (Overflow item : Overflow.values()) {
            if (!isOverflowVisible(item)) {
                continue;
            }
            if (slot == overflowIndex) {
                return item;
            }
            slot++;
        }
        return null;
    }

    @Override
    public int getCount() {
        int hidden = 0;
        if (!phoenixParamsVisible) {
            hidden++;
        }
        if (!juliaParamsVisible) {
            hidden++;
        }
        return CORE_COUNT + 1 + Overflow.values().length - hidden;
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
        boolean valueIndicator;
        Action action = actionAt(position);
        if (action != null) {
            labelRes = action.labelRes;
            iconRes = action.iconRes;
            contentDescriptionRes = action.contentDescriptionRes;
            valueIndicator = action.valueIndicator;
        } else {
            Overflow overflow = overflowAt(position);
            labelRes = overflow.labelRes;
            iconRes = overflow.iconRes;
            contentDescriptionRes = overflow.contentDescriptionRes;
            valueIndicator = overflow.valueIndicator;
        }

        label.setText(labelRes);
        icon.setImageResource(iconRes);
        ImageViewCompat.setImageTintList(
                icon,
                ContextCompat.getColorStateList(context, R.color.hud_icon_tint));
        row.setContentDescription(context.getString(contentDescriptionRes));

        if (valueIndicator) {
            indicator.setVisibility(View.VISIBLE);
            Overflow overflow = overflowAt(position);
            if (overflow == Overflow.PHOENIX_PARAMS) {
                indicator.setText(phoenixParamsIndicator);
            } else if (overflow == Overflow.JULIA_PARAMS) {
                indicator.setText(juliaParamsIndicator);
            } else {
                indicator.setText(
                        smoothOn ? R.string.status_overlay_smooth_on : R.string.status_overlay_smooth_off);
            }
        } else {
            indicator.setVisibility(View.GONE);
        }
        return row;
    }
}
