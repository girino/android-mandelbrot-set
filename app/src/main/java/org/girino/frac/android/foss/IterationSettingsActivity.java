package org.girino.frac.android.foss;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Escape-time iteration mode and values (issues #26 / #28). */
public class IterationSettingsActivity extends AppCompatActivity {

    public static final String EXTRA_DISABLE_ADAPTIVE = "disable_adaptive";

    private RadioGroup modeGroup;
    private View adaptiveRadio;
    private View adaptiveSection;
    private TextInputLayout fixedLayout;
    private TextInputLayout baseLayout;
    private TextInputLayout multiplierLayout;
    private TextInputLayout roundsLayout;
    private TextInputLayout capLayout;
    private TextInputEditText fixedValue;
    private TextInputEditText baseValue;
    private TextInputEditText multiplierValue;
    private TextInputEditText roundsValue;
    private TextInputEditText capValue;
    private boolean disableAdaptive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iteration_settings);

        MaterialToolbar toolbar = findViewById(R.id.iteration_toolbar);
        toolbar.setTitle(R.string.menu_iterations);
        toolbar.setNavigationOnClickListener(v -> finish());

        modeGroup = findViewById(R.id.iteration_mode_group);
        adaptiveRadio = findViewById(R.id.iteration_mode_adaptive);
        adaptiveSection = findViewById(R.id.iteration_adaptive_section);
        fixedLayout = findViewById(R.id.iteration_fixed_layout);
        baseLayout = findViewById(R.id.iteration_base_layout);
        multiplierLayout = findViewById(R.id.iteration_multiplier_layout);
        roundsLayout = findViewById(R.id.iteration_rounds_layout);
        capLayout = findViewById(R.id.iteration_cap_layout);
        fixedValue = findViewById(R.id.iteration_fixed_value);
        baseValue = findViewById(R.id.iteration_base_value);
        multiplierValue = findViewById(R.id.iteration_multiplier_value);
        roundsValue = findViewById(R.id.iteration_rounds_value);
        capValue = findViewById(R.id.iteration_cap_value);
        MaterialButton save = findViewById(R.id.iteration_save);
        MaterialButton resetDefaults = findViewById(R.id.iteration_reset_defaults);

        disableAdaptive = getIntent().getBooleanExtra(EXTRA_DISABLE_ADAPTIVE, false);
        if (disableAdaptive) {
            adaptiveRadio.setVisibility(View.GONE);
            adaptiveSection.setVisibility(View.GONE);
            roundsLayout.setVisibility(View.GONE);
            capLayout.setVisibility(View.GONE);
        }

        IterationSettings current = IterationSettingsStore.load(this);
        bind(current);
        modeGroup.setOnCheckedChangeListener((group, checkedId) -> updateEnabledFields());
        updateEnabledFields();

        resetDefaults.setOnClickListener(v -> {
            bind(IterationSettings.defaults());
            updateEnabledFields();
        });
        save.setOnClickListener(v -> saveAndFinish());

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        View actions = findViewById(R.id.iteration_actions);
        ViewCompat.setOnApplyWindowInsetsListener(actions, (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom);
            return insets;
        });
    }

    private void bind(IterationSettings settings) {
        if (settings.mode == IterationSettings.Mode.SCALE_WITH_ZOOM) {
            modeGroup.check(R.id.iteration_mode_zoom);
        } else if (settings.mode == IterationSettings.Mode.ADAPTIVE) {
            if (disableAdaptive) {
                modeGroup.check(R.id.iteration_mode_fixed);
            } else {
                modeGroup.check(R.id.iteration_mode_adaptive);
            }
        } else {
            modeGroup.check(R.id.iteration_mode_fixed);
        }
        fixedValue.setText(String.valueOf(settings.fixedMax));
        baseValue.setText(String.valueOf(settings.baseMax));
        multiplierValue.setText(formatMultiplier(settings.multiplier));
        roundsValue.setText(String.valueOf(settings.maxRounds));
        capValue.setText(String.valueOf(settings.absoluteCap));
    }

    private void updateEnabledFields() {
        int checked = modeGroup.getCheckedRadioButtonId();
        boolean fixed = checked == R.id.iteration_mode_fixed;
        boolean zoom = checked == R.id.iteration_mode_zoom;
        boolean adaptive = checked == R.id.iteration_mode_adaptive;

        // Pass-1 / fixed max field: used by Fixed and Adaptive.
        boolean fixedFieldOn = fixed || adaptive;
        fixedLayout.setEnabled(fixedFieldOn);
        fixedValue.setEnabled(fixedFieldOn);
        fixedLayout.setAlpha(fixedFieldOn ? 1f : 0.45f);

        baseLayout.setEnabled(zoom);
        baseValue.setEnabled(zoom);
        multiplierLayout.setEnabled(zoom);
        multiplierValue.setEnabled(zoom);
        float zoomAlpha = zoom ? 1f : 0.45f;
        baseLayout.setAlpha(zoomAlpha);
        multiplierLayout.setAlpha(zoomAlpha);

        roundsLayout.setEnabled(adaptive);
        roundsValue.setEnabled(adaptive);
        capLayout.setEnabled(adaptive);
        capValue.setEnabled(adaptive);
        float adaptiveAlpha = adaptive ? 1f : 0.45f;
        roundsLayout.setAlpha(adaptiveAlpha);
        capLayout.setAlpha(adaptiveAlpha);
    }

    private void saveAndFinish() {
        int checked = modeGroup.getCheckedRadioButtonId();
        IterationSettings.Mode mode;
        if (checked == R.id.iteration_mode_zoom) {
            mode = IterationSettings.Mode.SCALE_WITH_ZOOM;
        } else if (checked == R.id.iteration_mode_adaptive) {
            mode = disableAdaptive
                    ? IterationSettings.Mode.FIXED
                    : IterationSettings.Mode.ADAPTIVE;
        } else {
            mode = IterationSettings.Mode.FIXED;
        }

        Integer fixed = parseInt(fixedValue, IterationSettings.MIN_ITER, IterationSettings.MAX_ITER_CAP);
        Integer base = parseInt(baseValue, IterationSettings.MIN_ITER, IterationSettings.MAX_ITER_CAP);
        Double multiplier = parseDouble(multiplierValue);
        Integer rounds = parseInt(roundsValue, IterationSettings.MIN_ROUNDS, IterationSettings.MAX_ROUNDS);
        Integer cap = parseInt(capValue, IterationSettings.MIN_ITER, IterationSettings.MAX_ABSOLUTE_CAP);
        if (fixed == null || base == null || multiplier == null || rounds == null || cap == null) {
            Toast.makeText(this, R.string.iteration_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        if (multiplier < IterationSettings.MIN_MULTIPLIER
                || multiplier > IterationSettings.MAX_MULTIPLIER) {
            Toast.makeText(this, R.string.iteration_invalid_multiplier, Toast.LENGTH_SHORT).show();
            return;
        }

        if (IterationSettings.exceedsSoftWarn(mode, fixed, base, cap)) {
            Toast.makeText(this, R.string.iteration_soft_warn, Toast.LENGTH_LONG).show();
        }

        IterationSettings settings = new IterationSettings(
                mode, fixed, base, multiplier, rounds, cap);
        IterationSettingsStore.save(this, settings);
        setResult(RESULT_OK);
        finish();
    }

    private static Integer parseInt(TextInputEditText field, int min, int max) {
        CharSequence text = field.getText();
        if (text == null || TextUtils.getTrimmedLength(text) == 0) {
            return null;
        }
        try {
            int value = Integer.parseInt(text.toString().trim());
            if (value < min || value > max) {
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
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

    private static String formatMultiplier(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-6) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.valueOf(value);
    }
}
