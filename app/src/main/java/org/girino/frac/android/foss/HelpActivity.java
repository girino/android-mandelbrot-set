package org.girino.frac.android.foss;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

/** In-app usage guide (issue #22). */
public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_screen);

        MaterialToolbar toolbar = findViewById(R.id.info_toolbar);
        toolbar.setTitle(R.string.menu_help);
        toolbar.setNavigationOnClickListener(v -> finish());

        android.widget.TextView body = findViewById(R.id.info_body);
        body.setText(R.string.help_body);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.info_toolbar), (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
    }
}
