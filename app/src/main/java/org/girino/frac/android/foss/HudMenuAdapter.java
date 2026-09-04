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

    enum Action {
        FORMULA(R.string.menu_formula, R.drawable.ic_hud_formula, R.string.hud_formula_cd, false),
        RANDOM(R.string.menu_random, R.drawable.ic_hud_random, R.string.hud_random_cd, false),
        PALETTE(R.string.menu_palette, R.drawable.ic_hud_palette, R.string.hud_palette_cd, false),
        SMOOTH(R.string.menu_smooth_palette, R.drawable.ic_hud_smooth, R.string.hud_smooth_cd, true),
        ITERATIONS(R.string.menu_iterations, R.drawable.ic_hud_iterations, R.string.hud_iterations_cd, false),
        FORMULA_PARAMS(
                R.string.menu_formula_params,
                R.drawable.ic_hud_julia_params,
                R.string.hud_formula_params_cd,
                true),
        HELP(R.string.menu_help, R.drawable.ic_hud_help, R.string.hud_help_cd, false),
        ABOUT(R.string.menu_about, R.drawable.ic_hud_about, R.string.hud_about_cd, false);

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

    private final LayoutInflater inflater;
    private final Context context;
    private final boolean smoothOn;
    private final boolean formulaParamsVisible;
    private final String formulaParamsIndicator;

    HudMenuAdapter(
            Context context,
            boolean smoothOn,
            boolean formulaParamsVisible,
            String formulaParamsIndicator) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.smoothOn = smoothOn;
        this.formulaParamsVisible = formulaParamsVisible;
        this.formulaParamsIndicator = formulaParamsIndicator != null ? formulaParamsIndicator : "";
    }

    boolean isSmooth(int position) {
        return actionAt(position) == Action.SMOOTH;
    }

    boolean isIterations(int position) {
        return actionAt(position) == Action.ITERATIONS;
    }

    boolean isRandom(int position) {
        return actionAt(position) == Action.RANDOM;
    }

    boolean isFormulaParams(int position) {
        return actionAt(position) == Action.FORMULA_PARAMS;
    }

    boolean isHelp(int position) {
        return actionAt(position) == Action.HELP;
    }

    boolean isAbout(int position) {
        return actionAt(position) == Action.ABOUT;
    }

    Action actionAt(int position) {
        int slot = 0;
        for (Action item : Action.values()) {
            if (!isVisible(item)) {
                continue;
            }
            if (slot == position) {
                return item;
            }
            slot++;
        }
        return null;
    }

    private boolean isVisible(Action item) {
        if (item == Action.FORMULA_PARAMS) {
            return formulaParamsVisible;
        }
        return true;
    }

    @Override
    public int getCount() {
        int hidden = formulaParamsVisible ? 0 : 1;
        return Action.values().length - hidden;
    }

    @Override
    public Object getItem(int position) {
        return actionAt(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (!(row instanceof ViewGroup)) {
            row = inflater.inflate(R.layout.hud_menu_item, parent, false);
        }
        ImageView icon = row.findViewById(R.id.hud_menu_icon);
        TextView label = row.findViewById(R.id.hud_menu_label);
        TextView indicator = row.findViewById(R.id.hud_menu_indicator);

        Action action = actionAt(position);
        label.setText(action.labelRes);
        icon.setImageResource(action.iconRes);
        ImageViewCompat.setImageTintList(
                icon,
                ContextCompat.getColorStateList(context, R.color.hud_icon_tint));
        row.setContentDescription(context.getString(action.contentDescriptionRes));

        if (action.valueIndicator) {
            indicator.setVisibility(View.VISIBLE);
            if (action == Action.FORMULA_PARAMS) {
                indicator.setText(formulaParamsIndicator);
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
