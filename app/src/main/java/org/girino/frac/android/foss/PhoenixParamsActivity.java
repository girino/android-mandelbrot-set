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

/** Custom Phoenix distortion p = re + im*i (experiment). */
public class PhoenixParamsActivity extends AppCompatActivity {

    public static final String EXTRA_P_RE = "p_re";
    public static final String EXTRA_P_IM = "p_im";

    private TextInputEditText pReValue;
    private TextInputEditText pImValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phoenix_params);

        MaterialToolbar toolbar = findViewById(R.id.phoenix_toolbar);
        toolbar.setTitle(R.string.phoenix_custom_title);
        toolbar.setNavigationOnClickListener(v -> finish());

        pReValue = findViewById(R.id.phoenix_p_re_value);
        pImValue = findViewById(R.id.phoenix_p_im_value);
        MaterialButton save = findViewById(R.id.phoenix_save);

        PhoenixParamsStore.Params current = PhoenixParamsStore.load(this);
        double initialRe = getIntent().getDoubleExtra(EXTRA_P_RE, current.pRe);
        double initialIm = getIntent().getDoubleExtra(EXTRA_P_IM, current.pIm);
        pReValue.setText(PhoenixPresetCatalog.formatEditableComponent(initialRe));
        pImValue.setText(PhoenixPresetCatalog.formatEditableComponent(initialIm));

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
        Double pRe = parseDouble(pReValue);
        Double pIm = parseDouble(pImValue);
        if (pRe == null || pIm == null) {
            Toast.makeText(this, R.string.phoenix_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        PhoenixParamsStore.save(this, pRe, pIm);
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
