package org.girino.frac.android.foss;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ToggleButton;

import org.girino.frac.operators.FractalOperator;
import org.girino.frac.palettes.PaletteProvider;

/**
 * Main screen: full-bleed fractal with a compact bottom HUD for formula,
 * palette, smooth coloring, and reset (issue #5). Overflow menu keeps Zoom
 * and Exit as secondary actions.
 */
public class MandelbrotActivity extends Activity {
    private static final int SELECT_OPERATOR = 0;
    private static final int SELECT_PALETTE = 1;

    private MandelbrotView view;
    private ToggleButton hudSmooth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mandelbrot);
        view = findViewById(R.id.mandelbrot_view);
        hudSmooth = findViewById(R.id.hud_smooth);

        Button hudFormula = findViewById(R.id.hud_formula);
        Button hudPalette = findViewById(R.id.hud_palette);
        Button hudReset = findViewById(R.id.hud_reset);

        hudFormula.setOnClickListener(v -> openFormulaPicker());
        hudPalette.setOnClickListener(v -> openPalettePicker());
        hudReset.setOnClickListener(v -> view.reset());
        hudSmooth.setOnClickListener(v -> {
            // ToggleButton flips its checked state before the click listener;
            // align the view flag to that state (smooth() also toggles).
            if (hudSmooth.isChecked() != view.isSmooth()) {
                view.smooth();
            }
            syncSmoothControls();
        });
        syncSmoothControls();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncSmoothControls();
        view.start();
    }

    @Override
    protected void onPause() {
        view.stop();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Menu.FIRST, 0, R.string.menu_formula);
        menu.add(0, Menu.FIRST + 1, 1, R.string.menu_palette);
        menu.add(0, Menu.FIRST + 2, 2, R.string.menu_smooth_palette)
                .setCheckable(true)
                .setChecked(view.isSmooth());
        menu.add(0, Menu.FIRST + 3, 3, R.string.menu_zoom);
        menu.add(0, Menu.FIRST + 4, 4, R.string.menu_reset);
        menu.add(0, Menu.FIRST + 5, 5, R.string.menu_exit);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem smoothItem = menu.findItem(Menu.FIRST + 2);
        if (smoothItem != null) {
            smoothItem.setChecked(view.isSmooth());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId() - Menu.FIRST) {
            case 0:
                openFormulaPicker();
                return true;
            case 1:
                openPalettePicker();
                return true;
            case 2:
                view.smooth();
                item.setChecked(view.isSmooth());
                syncSmoothControls();
                return true;
            case 3:
                view.zoom();
                return true;
            case 4:
                view.reset();
                return true;
            case 5:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_OPERATOR && resultCode >= Activity.RESULT_FIRST_USER) {
            FractalOperator operator = OperatorsListActivity.getOperator(
                    resultCode - Activity.RESULT_FIRST_USER);
            view.setOper(operator);
            view.reset();
        } else if (requestCode == SELECT_PALETTE && resultCode >= Activity.RESULT_FIRST_USER) {
            PaletteProvider provider = PalettesListActivity.getPalette(
                    resultCode - Activity.RESULT_FIRST_USER);
            view.setPalette(provider);
            view.start();
        }
    }

    private void openFormulaPicker() {
        view.stop();
        startActivityForResult(new Intent(this, OperatorsListActivity.class), SELECT_OPERATOR);
    }

    private void openPalettePicker() {
        view.stop();
        startActivityForResult(new Intent(this, PalettesListActivity.class), SELECT_PALETTE);
    }

    private void syncSmoothControls() {
        if (hudSmooth != null) {
            hudSmooth.setChecked(view.isSmooth());
        }
    }
}
