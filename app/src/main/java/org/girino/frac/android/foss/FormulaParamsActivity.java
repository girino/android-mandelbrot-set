package org.girino.frac.android.foss;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Julia c and Phoenix p on one screen; fields enabled per active formula. */
public class FormulaParamsActivity extends AppCompatActivity {

    public static final String EXTRA_OPERATOR_INDEX = "operator_index";
    public static final String EXTRA_C_RE = "c_re";
    public static final String EXTRA_C_IM = "c_im";
    public static final String EXTRA_P_RE = "p_re";
    public static final String EXTRA_P_IM = "p_im";

    private static final float DISABLED_ALPHA = 0.38f;

    private int operatorIndex;
    private boolean cEnabled;
    private boolean pEnabled;

    private View cBlock;
    private View pBlock;
    private TextInputLayout cReLayout;
    private TextInputLayout cImLayout;
    private TextInputLayout pReLayout;
    private TextInputLayout pImLayout;
    private TextInputEditText cReValue;
    private TextInputEditText cImValue;
    private TextInputEditText pReValue;
    private TextInputEditText pImValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formula_params);

        operatorIndex = getIntent().getIntExtra(EXTRA_OPERATOR_INDEX, FormulaCatalog.JULIA_INDEX);
        cEnabled = usesCParam(operatorIndex);
        pEnabled = usesPParam(operatorIndex);

        MaterialToolbar toolbar = findViewById(R.id.formula_params_toolbar);
        toolbar.setTitle(R.string.formula_params_title);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_formula_presets) {
                openPresetsPicker();
                return true;
            }
            return false;
        });

        TextView summary = findViewById(R.id.formula_params_summary);
        summary.setText(summaryResForOperator(operatorIndex));

        cBlock = findViewById(R.id.formula_c_block);
        pBlock = findViewById(R.id.formula_p_block);
        cReLayout = findViewById(R.id.formula_c_re_layout);
        cImLayout = findViewById(R.id.formula_c_im_layout);
        pReLayout = findViewById(R.id.formula_p_re_layout);
        pImLayout = findViewById(R.id.formula_p_im_layout);
        cReValue = findViewById(R.id.formula_c_re_value);
        cImValue = findViewById(R.id.formula_c_im_value);
        pReValue = findViewById(R.id.formula_p_re_value);
        pImValue = findViewById(R.id.formula_p_im_value);
        MaterialButton save = findViewById(R.id.formula_params_save);

        JuliaParamsStore.Params julia = JuliaParamsStore.load(this);
        PhoenixParamsStore.Params phoenix = PhoenixParamsStore.load(this);
        double initialCRe = getIntent().getDoubleExtra(EXTRA_C_RE, julia.cRe);
        double initialCIm = getIntent().getDoubleExtra(EXTRA_C_IM, julia.cIm);
        double initialPRe = getIntent().getDoubleExtra(EXTRA_P_RE, phoenix.pRe);
        double initialPIm = getIntent().getDoubleExtra(EXTRA_P_IM, phoenix.pIm);

        cReValue.setText(JuliaPresetCatalog.formatEditableComponent(initialCRe));
        cImValue.setText(JuliaPresetCatalog.formatEditableComponent(initialCIm));
        pReValue.setText(PhoenixPresetCatalog.formatEditableComponent(initialPRe));
        pImValue.setText(PhoenixPresetCatalog.formatEditableComponent(initialPIm));

        setBlockEnabled(cBlock, cReLayout, cImLayout, cReValue, cImValue, cEnabled);
        setBlockEnabled(pBlock, pReLayout, pImLayout, pReValue, pImValue, pEnabled);

        save.setOnClickListener(v -> saveAndFinish());

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        ViewGroup.MarginLayoutParams saveMargins =
                (ViewGroup.MarginLayoutParams) save.getLayoutParams();
        final int saveBottomMargin = saveMargins.bottomMargin;
        ViewCompat.setOnApplyWindowInsetsListener(save, (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.bottomMargin = saveBottomMargin + bottom;
            v.setLayoutParams(params);
            return insets;
        });
    }

    static boolean usesCParam(int operatorIndex) {
        return operatorIndex == FormulaCatalog.JULIA_INDEX
                || operatorIndex == FormulaCatalog.JULIA_PHOENIX_INDEX;
    }

    static boolean usesPParam(int operatorIndex) {
        return operatorIndex == FormulaCatalog.PHOENIX_INDEX
                || operatorIndex == FormulaCatalog.JULIA_PHOENIX_INDEX;
    }

    static boolean showsFormulaParamsMenu(int operatorIndex) {
        return usesCParam(operatorIndex) || usesPParam(operatorIndex);
    }

    private static int summaryResForOperator(int operatorIndex) {
        if (operatorIndex == FormulaCatalog.JULIA_PHOENIX_INDEX) {
            return R.string.formula_params_summary_julia_phoenix;
        }
        if (operatorIndex == FormulaCatalog.JULIA_INDEX) {
            return R.string.formula_params_summary_julia;
        }
        return R.string.formula_params_summary_phoenix;
    }

    private static void setBlockEnabled(
            View block,
            TextInputLayout reLayout,
            TextInputLayout imLayout,
            TextInputEditText reValue,
            TextInputEditText imValue,
            boolean enabled) {
        block.setAlpha(enabled ? 1f : DISABLED_ALPHA);
        reLayout.setEnabled(enabled);
        imLayout.setEnabled(enabled);
        reValue.setEnabled(enabled);
        imValue.setEnabled(enabled);
    }

    private void openPresetsPicker() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_picker, null);
        BottomSheetPickerHelper.prepareList(sheet.findViewById(R.id.picker_list));
        dialog.setContentView(sheet);

        TextView title = sheet.findViewById(R.id.picker_title);
        ListView list = sheet.findViewById(R.id.picker_list);
        title.setText(R.string.select_formula_params_presets);

        PresetPickerEntry[] entries = buildPresetEntries();
        String[] labels = new String[entries.length];
        for (int i = 0; i < entries.length; i++) {
            labels[i] = entries[i].label;
        }
        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
        list.setChoiceMode(ListView.CHOICE_MODE_NONE);
        list.setOnItemClickListener((parent, row, position, id) -> {
            dialog.dismiss();
            applyPresetEntry(entries[position]);
        });
        dialog.show();
    }

    private PresetPickerEntry[] buildPresetEntries() {
        int cCount = cEnabled ? JuliaPresetCatalog.presetCount() : 0;
        int pCount = pEnabled ? PhoenixPresetCatalog.presetCount() : 0;
        PresetPickerEntry[] entries = new PresetPickerEntry[cCount + pCount];
        int index = 0;
        if (cEnabled) {
            for (int i = 0; i < JuliaPresetCatalog.presetCount(); i++) {
                JuliaPresetCatalog.Preset preset = JuliaPresetCatalog.getPreset(i);
                entries[index++] = new PresetPickerEntry(
                        getString(R.string.formula_preset_c_row, getString(preset.labelRes)),
                        preset.cRe,
                        preset.cIm,
                        null,
                        null);
            }
        }
        if (pEnabled) {
            for (int i = 0; i < PhoenixPresetCatalog.presetCount(); i++) {
                PhoenixPresetCatalog.Preset preset = PhoenixPresetCatalog.getPreset(i);
                entries[index++] = new PresetPickerEntry(
                        getString(R.string.formula_preset_p_row, getString(preset.labelRes)),
                        null,
                        null,
                        preset.pRe,
                        preset.pIm);
            }
        }
        return entries;
    }

    private void applyPresetEntry(PresetPickerEntry entry) {
        if (entry.cRe != null && entry.cIm != null) {
            cReValue.setText(JuliaPresetCatalog.formatEditableComponent(entry.cRe));
            cImValue.setText(JuliaPresetCatalog.formatEditableComponent(entry.cIm));
        }
        if (entry.pRe != null && entry.pIm != null) {
            pReValue.setText(PhoenixPresetCatalog.formatEditableComponent(entry.pRe));
            pImValue.setText(PhoenixPresetCatalog.formatEditableComponent(entry.pIm));
        }
    }

    private void saveAndFinish() {
        Double cRe = null;
        Double cIm = null;
        Double pRe = null;
        Double pIm = null;

        if (cEnabled) {
            cRe = parseDouble(cReValue);
            cIm = parseDouble(cImValue);
            if (cRe == null || cIm == null) {
                Toast.makeText(this, R.string.formula_params_invalid_c, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (pEnabled) {
            pRe = parseDouble(pReValue);
            pIm = parseDouble(pImValue);
            if (pRe == null || pIm == null) {
                Toast.makeText(this, R.string.formula_params_invalid_p, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (cEnabled) {
            JuliaParamsStore.save(this, cRe, cIm);
        }
        if (pEnabled) {
            PhoenixParamsStore.save(this, pRe, pIm);
        }
        setResult(RESULT_OK);
        finish();
    }

    private static Double parseDouble(TextInputEditText field) {
        CharSequence text = field.getText();
        if (text == null || TextUtils.getTrimmedLength(text) == 0) {
            return null;
        }
        try {
            return Double.parseDouble(text.toString().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final class PresetPickerEntry {
        final String label;
        final Double cRe;
        final Double cIm;
        final Double pRe;
        final Double pIm;

        PresetPickerEntry(String label, Double cRe, Double cIm, Double pRe, Double pIm) {
            this.label = label;
            this.cRe = cRe;
            this.cIm = cIm;
            this.pRe = pRe;
            this.pIm = pIm;
        }
    }
}
