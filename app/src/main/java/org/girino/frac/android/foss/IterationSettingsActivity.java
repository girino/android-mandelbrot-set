package org.girino.frac.android.foss;

import android.os.Bundle;
import android.text.TextUtils;
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

/** Escape-time iteration mode and values (issue #26). */
public class IterationSettingsActivity extends AppCompatActivity {

    private RadioGroup modeGroup;
    private TextInputLayout fixedLayout;
    private TextInputLayout baseLayout;
    private TextInputLayout multiplierLayout;
    private TextInputEditText fixedValue;
    private TextInputEditText baseValue;
    private TextInputEditText multiplierValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iteration_settings);

        MaterialToolbar toolbar = findViewById(R.id.iteration_toolbar);
        toolbar.setTitle(R.string.menu_iterations);
        toolbar.setNavigationOnClickListener(v -> finish());

        modeGroup = findViewById(R.id.iteration_mode_group);
        fixedLayout = findViewById(R.id.iteration_fixed_layout);
        baseLayout = findViewById(R.id.iteration_base_layout);
        multiplierLayout = findViewById(R.id.iteration_multiplier_layout);
        fixedValue = findViewById(R.id.iteration_fixed_value);
        baseValue = findViewById(R.id.iteration_base_value);
        multiplierValue = findViewById(R.id.iteration_multiplier_value);
        MaterialButton save = findViewById(R.id.iteration_save);

        IterationSettings current = IterationSettingsStore.load(this);
        bind(current);
        modeGroup.setOnCheckedChangeListener((group, checkedId) -> updateEnabledFields());
        updateEnabledFields();

        save.setOnClickListener(v -> saveAndFinish());

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(save, (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom);
            return insets;
        });
    }

    private void bind(IterationSettings settings) {
        if (settings.mode == IterationSettings.Mode.SCALE_WITH_ZOOM) {
            modeGroup.check(R.id.iteration_mode_zoom);
        } else {
            modeGroup.check(R.id.iteration_mode_fixed);
        }
        fixedValue.setText(String.valueOf(settings.fixedMax));
        baseValue.setText(String.valueOf(settings.baseMax));
        multiplierValue.setText(formatMultiplier(settings.multiplier));
    }

    private void updateEnabledFields() {
        boolean fixed = modeGroup.getCheckedRadioButtonId() == R.id.iteration_mode_fixed;
        fixedLayout.setEnabled(fixed);
        fixedValue.setEnabled(fixed);
        baseLayout.setEnabled(!fixed);
        baseValue.setEnabled(!fixed);
        multiplierLayout.setEnabled(!fixed);
        multiplierValue.setEnabled(!fixed);
        float alpha = fixed ? 1f : 0.45f;
        float zoomAlpha = fixed ? 0.45f : 1f;
        fixedLayout.setAlpha(alpha);
        baseLayout.setAlpha(zoomAlpha);
        multiplierLayout.setAlpha(zoomAlpha);
    }

    private void saveAndFinish() {
        IterationSettings.Mode mode = modeGroup.getCheckedRadioButtonId() == R.id.iteration_mode_zoom
                ? IterationSettings.Mode.SCALE_WITH_ZOOM
                : IterationSettings.Mode.FIXED;

        Integer fixed = parseInt(fixedValue);
        Integer base = parseInt(baseValue);
        Double multiplier = parseDouble(multiplierValue);
        if (fixed == null || base == null || multiplier == null) {
            Toast.makeText(this, R.string.iteration_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        if (multiplier < IterationSettings.MIN_MULTIPLIER
                || multiplier > IterationSettings.MAX_MULTIPLIER) {
            Toast.makeText(this, R.string.iteration_invalid_multiplier, Toast.LENGTH_SHORT).show();
            return;
        }

        IterationSettings settings = new IterationSettings(mode, fixed, base, multiplier);
        IterationSettingsStore.save(this, settings);
        setResult(RESULT_OK);
        finish();
    }

    private static Integer parseInt(TextInputEditText field) {
        CharSequence text = field.getText();
        if (text == null || TextUtils.getTrimmedLength(text) == 0) {
            return null;
        }
        try {
            int value = Integer.parseInt(text.toString().trim());
            if (value < IterationSettings.MIN_ITER || value > IterationSettings.MAX_ITER_CAP) {
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
