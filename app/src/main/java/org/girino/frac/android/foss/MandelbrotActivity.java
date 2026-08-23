package org.girino.frac.android.foss;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;

/**
 * Main screen: full-bleed fractal with a compact bottom HUD for formula,
 * palette, smooth coloring, reset, and zoom. Material dark theme and
 * bottom-sheet pickers (issues #10 / #13). Thin top progress bar tracks
 * progressive render samples (issue #9).
 */
public class MandelbrotActivity extends AppCompatActivity {
    /** Delay before showing the bar so fast renders do not flash (issue #9). */
    private static final long RENDER_PROGRESS_SHOW_DELAY_MS = 150L;
    /** Default matches MandelbrotView initial operator (Mandelbrot Set). */
    private static final int DEFAULT_OPERATOR_INDEX = 0;
    /** Default matches MandelbrotView initial palette (HSB / Rainbow 1). */
    private static final int DEFAULT_PALETTE_INDEX = 3;

    private MandelbrotView view;
    private ToggleButton hudSmooth;
    private ProgressBar renderProgress;
    private int operatorIndex = DEFAULT_OPERATOR_INDEX;
    private int paletteIndex = DEFAULT_PALETTE_INDEX;
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

    private void openFormulaPicker() {
        showPickerSheet(
                R.string.select_formula,
                FormulaCatalog.labels(),
                operatorIndex,
                index -> {
                    operatorIndex = index;
                    view.setOper(FormulaCatalog.get(index));
                    view.reset();
                });
    }

    private void openPalettePicker() {
        showPickerSheet(
                R.string.select_palette,
                PaletteCatalog.labels(),
                paletteIndex,
                index -> {
                    paletteIndex = index;
                    view.setPalette(PaletteCatalog.get(index));
                    view.start();
                });
    }

    private void showPickerSheet(
            int titleRes,
            String[] labels,
            int checkedIndex,
            PickerSelectionListener listener) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_picker, null);
        TextView title = sheet.findViewById(R.id.picker_title);
        ListView list = sheet.findViewById(R.id.picker_list);
        title.setText(titleRes);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_single_choice,
                labels);
        list.setAdapter(adapter);
        int safeChecked = Math.max(0, Math.min(checkedIndex, labels.length - 1));
        list.setItemChecked(safeChecked, true);
        list.setSelection(safeChecked);
        list.setOnItemClickListener((parent, view1, position, id) -> {
            listener.onSelected(position);
            dialog.dismiss();
        });
        dialog.setContentView(sheet);
        dialog.show();
    }

    private void syncSmoothControls() {
        if (hudSmooth != null) {
            hudSmooth.setChecked(view.isSmooth());
        }
    }

    private interface PickerSelectionListener {
        void onSelected(int index);
    }
}
