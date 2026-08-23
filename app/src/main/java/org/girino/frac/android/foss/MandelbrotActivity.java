package org.girino.frac.android.foss;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ToggleButton;

import org.girino.frac.operators.FractalOperator;
import org.girino.frac.palettes.PaletteProvider;

/**
 * Main screen: full-bleed fractal with a compact bottom HUD for formula,
 * palette, smooth coloring, and reset (issue #5), plus zoom in/out (issue #6).
 * Overflow menu mirrors those actions and keeps Exit. Thin top progress bar
 * tracks progressive render samples (issue #9).
 */
public class MandelbrotActivity extends Activity {
    private static final int SELECT_OPERATOR = 0;
    private static final int SELECT_PALETTE = 1;
    /** Delay before showing the bar so fast renders do not flash (issue #9). */
    private static final long RENDER_PROGRESS_SHOW_DELAY_MS = 150L;

    private MandelbrotView view;
    private ToggleButton hudSmooth;
    private ProgressBar renderProgress;
    private final Runnable showRenderProgress = () -> {
        if (renderProgress != null) {
            renderProgress.setVisibility(View.VISIBLE);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mandelbrot);
        view = findViewById(R.id.mandelbrot_view);
        hudSmooth = findViewById(R.id.hud_smooth);
        renderProgress = findViewById(R.id.render_progress);
        View hudBar = findViewById(R.id.hud_bar);
        applyWindowInsets(hudBar, renderProgress);

        view.setRenderBusyListener(new MandelbrotView.RenderBusyListener() {
            @Override
            public void onRenderBusy(boolean busy) {
                MandelbrotActivity.this.onRenderBusy(busy);
            }

            @Override
            public void onRenderProgress(int completed, int total) {
                MandelbrotActivity.this.onRenderProgress(completed, total);
            }
        });

        Button hudFormula = findViewById(R.id.hud_formula);
        Button hudPalette = findViewById(R.id.hud_palette);
        Button hudZoomOut = findViewById(R.id.hud_zoom_out);
        Button hudZoomIn = findViewById(R.id.hud_zoom_in);
        Button hudReset = findViewById(R.id.hud_reset);

        hudFormula.setOnClickListener(v -> openFormulaPicker());
        hudPalette.setOnClickListener(v -> openPalettePicker());
        hudZoomOut.setOnClickListener(v -> view.zoomOut());
        hudZoomIn.setOnClickListener(v -> view.zoomIn());
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

    /**
     * Keep the HUD above the system navigation bar and the progress bar
     * below the status bar (edge-to-edge otherwise overlaps system chrome).
     */
    private void applyWindowInsets(View hudBar, View progressBar) {
        final int hudLeft = hudBar.getPaddingLeft();
        final int hudTop = hudBar.getPaddingTop();
        final int hudRight = hudBar.getPaddingRight();
        final int hudBottom = hudBar.getPaddingBottom();
        View root = findViewById(R.id.mandelbrot_root);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int navBottom;
            int statusTop;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                navBottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                statusTop = insets.getInsets(WindowInsets.Type.statusBars()).top;
            } else {
                navBottom = insets.getSystemWindowInsetBottom();
                statusTop = insets.getSystemWindowInsetTop();
            }
            hudBar.setPadding(hudLeft, hudTop, hudRight, hudBottom + navBottom);
            progressBar.setTranslationY(statusTop);
            return insets;
        });
        root.requestApplyInsets();
    }

    private void onRenderBusy(boolean busy) {
        if (renderProgress == null) {
            return;
        }
        renderProgress.removeCallbacks(showRenderProgress);
        if (busy) {
            renderProgress.setProgress(0);
            renderProgress.postDelayed(showRenderProgress, RENDER_PROGRESS_SHOW_DELAY_MS);
        } else {
            renderProgress.setVisibility(View.GONE);
        }
    }

    private void onRenderProgress(int completed, int total) {
        if (renderProgress == null) {
            return;
        }
        int max = Math.max(total, 1);
        if (renderProgress.getMax() != max) {
            renderProgress.setMax(max);
        }
        renderProgress.setProgress(Math.min(completed, max));
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
        onRenderBusy(false);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Menu.FIRST, 0, R.string.menu_formula);
        menu.add(0, Menu.FIRST + 1, 1, R.string.menu_palette);
        menu.add(0, Menu.FIRST + 2, 2, R.string.menu_smooth_palette)
                .setCheckable(true)
                .setChecked(view.isSmooth());
        menu.add(0, Menu.FIRST + 3, 3, R.string.menu_zoom_in);
        menu.add(0, Menu.FIRST + 4, 4, R.string.menu_zoom_out);
        menu.add(0, Menu.FIRST + 5, 5, R.string.menu_reset);
        menu.add(0, Menu.FIRST + 6, 6, R.string.menu_exit);
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
                view.zoomIn();
                return true;
            case 4:
                view.zoomOut();
                return true;
            case 5:
                view.reset();
                return true;
            case 6:
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
