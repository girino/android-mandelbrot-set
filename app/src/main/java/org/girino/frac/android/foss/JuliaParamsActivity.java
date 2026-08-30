package org.girino.frac.android.foss;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/** Custom Julia seed c (fixed parameter). */
public class JuliaParamsActivity extends AppCompatActivity {

    public static final String EXTRA_C_RE = "c_re";
    public static final String EXTRA_C_IM = "c_im";

    private TextInputEditText cReValue;
    private TextInputEditText cImValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_julia_params);

        MaterialToolbar toolbar = findViewById(R.id.julia_toolbar);
        toolbar.setTitle(R.string.julia_custom_title);
        toolbar.setNavigationOnClickListener(v -> finish());

        cReValue = findViewById(R.id.julia_c_re_value);
        cImValue = findViewById(R.id.julia_c_im_value);
        MaterialButton save = findViewById(R.id.julia_save);

        JuliaParamsStore.Params current = JuliaParamsStore.load(this);
        double initialRe = getIntent().getDoubleExtra(EXTRA_C_RE, current.cRe);
        double initialIm = getIntent().getDoubleExtra(EXTRA_C_IM, current.cIm);
        cReValue.setText(JuliaPresetCatalog.formatEditableComponent(initialRe));
        cImValue.setText(JuliaPresetCatalog.formatEditableComponent(initialIm));

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

    private void saveAndFinish() {
        Double cRe = parseDouble(cReValue);
        Double cIm = parseDouble(cImValue);
        if (cRe == null || cIm == null) {
            Toast.makeText(this, R.string.julia_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        JuliaParamsStore.save(this, cRe, cIm);
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
}
