package org.girino.frac.android.foss;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

/**
 * Main screen: full-bleed fractal with a compact bottom HUD for formula,
 * palette, smooth coloring, reset, and zoom. Material dark theme and
 * bottom-sheet pickers (issues #10 / #13). Thin top progress bar tracks
 * progressive render samples (issue #9). Long-press shows complex
 * coordinates (issue #11). Options menu removed with NoActionBar (issue #12);
 * exit via system back / recents. Edge-to-edge fractal under system bars
 * (issue #14). Corner status overlay for formula + smooth (issue #17).
 * Export viewport as PNG via share sheet or gallery save (issue #18).
 * Icon HUD bar + hamburger overflow menu (issues #29 / #46).
 * Saves viewport on rotation / process recreate (issue #21).
 * Persists last session across cold start (issue #19).
 * Help and About screens from the menu (issue #22).
 * Iteration settings: fixed max or scale-with-zoom (issue #26).
 */
public class MandelbrotActivity extends AppCompatActivity {
    private static final String STATE_VIEWPORT = "viewport_session";
    private static final String STATE_OVERLAY_VISIBLE = "status_overlay_visible";
    /** Delay before showing the bar so fast renders do not flash (issue #9). */
    private static final long RENDER_PROGRESS_SHOW_DELAY_MS = 150L;
    /** Default matches MandelbrotView initial operator (Mandelbrot Set). */
    private static final int DEFAULT_OPERATOR_INDEX = 0;
    /** Default matches MandelbrotView initial palette (HSB / RGB). */
    private static final int DEFAULT_PALETTE_INDEX = 3;

    private MandelbrotView view;
    private ImageButton hudExport;
    private ProgressBar renderProgress;
    private Snackbar coordinateSnackbar;
    private TextView statusOverlay;
    private TextView statusOverlayChip;
    private int operatorIndex = DEFAULT_OPERATOR_INDEX;
    private int paletteIndex = DEFAULT_PALETTE_INDEX;
    /** Set when pausing mid-render; cleared after resume restart. */
    private boolean resumeInterruptedRender;
    private Bitmap pendingSaveBitmap;
    private final ActivityResultLauncher<String> saveDocumentLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("image/png"),
                    this::onLegacySaveResult);
    private final ActivityResultLauncher<Intent> iterationSettingsLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            applyIterationSettings(IterationSettingsStore.load(this));
                        }
                    });
    private final Runnable showRenderProgress = () -> {
        if (renderProgress != null) {
            renderProgress.setVisibility(View.VISIBLE);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mandelbrot);
        applyLightSystemBarIcons();

        view = findViewById(R.id.mandelbrot_view);
        hudExport = findViewById(R.id.hud_export);
        renderProgress = findViewById(R.id.render_progress);
        statusOverlay = findViewById(R.id.status_overlay);
        statusOverlayChip = findViewById(R.id.status_overlay_chip);
        View hudBar = findViewById(R.id.hud_bar);
        applyWindowInsets(hudBar, renderProgress, statusOverlay, statusOverlayChip);

        statusOverlay.setOnClickListener(v -> setStatusOverlayVisible(false));
        statusOverlayChip.setOnClickListener(v -> setStatusOverlayVisible(true));

        restoreSessionState(savedInstanceState);
        applyIterationSettings(IterationSettingsStore.load(this));

        view.setRenderBusyListener(new MandelbrotView.RenderBusyListener() {
            @Override
            public void onRenderBusy(boolean busy) {
                MandelbrotActivity.this.onRenderBusy(busy);
            }

            @Override
            public void onRenderProgress(int completed, int total) {
                MandelbrotActivity.this.onRenderProgress(completed, total);
            }

            @Override
            public void onRenderIndeterminate(boolean indeterminate) {
                MandelbrotActivity.this.onRenderIndeterminate(indeterminate);
            }

            @Override
            public void onEffectiveMaxIterChanged() {
                refreshStatusOverlay();
            }
        });
        view.setCoordinateReadoutListener(new MandelbrotView.CoordinateReadoutListener() {
            @Override
            public void onCoordinateReadout(double real, double imag) {
                showCoordinateReadout(real, imag);
            }

            @Override
            public void onCoordinateReadoutDismiss() {
                dismissCoordinateReadout();
            }
        });

        ImageButton hudZoomIn = findViewById(R.id.hud_zoom_in);
        ImageButton hudZoomOut = findViewById(R.id.hud_zoom_out);
        ImageButton hudReset = findViewById(R.id.hud_reset);
        ImageButton hudMenu = findViewById(R.id.hud_menu);

        hudZoomIn.setOnClickListener(v -> {
            view.zoomIn();
            persistCurrentSession();
            refreshStatusOverlay();
        });
        hudZoomOut.setOnClickListener(v -> {
            view.zoomOut();
            persistCurrentSession();
            refreshStatusOverlay();
        });
        hudReset.setOnClickListener(v -> performFullReset());
        hudExport.setOnClickListener(v -> openExportSheet());
        hudMenu.setOnClickListener(v -> openHudMenu());
        refreshStatusOverlay();
    }

    private void applyIterationSettings(IterationSettings settings) {
        view.setIterationSettings(settings);
        refreshStatusOverlay();
    }

    /** Viewport home (HUD Reset). Iteration settings unchanged — use Iterations screen. */
    private void performFullReset() {
        view.reset();
        persistCurrentSession();
        refreshStatusOverlay();
    }

    private void toggleSmooth() {
        view.smooth();
        persistCurrentSession();
        refreshStatusOverlay();
    }

    private void openHudMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_picker, null);
        TextView title = sheet.findViewById(R.id.picker_title);
        ListView list = sheet.findViewById(R.id.picker_list);
        title.setText(R.string.hud_menu_title);
        HudMenuAdapter adapter = new HudMenuAdapter(this, view.isSmooth());
        list.setAdapter(adapter);
        list.setChoiceMode(ListView.CHOICE_MODE_NONE);
        list.setOnItemClickListener((parent, row, position, id) -> {
            if (HudMenuAdapter.isSectionHeader(position)) {
                return;
            }
            dialog.dismiss();
            if (HudMenuAdapter.isSmooth(position)) {
                toggleSmooth();
                return;
            }
            if (HudMenuAdapter.isIterations(position)) {
                iterationSettingsLauncher.launch(
                        new Intent(this, IterationSettingsActivity.class));
                return;
            }
            if (HudMenuAdapter.isHelp(position)) {
                startActivity(new Intent(this, HelpActivity.class));
                return;
            }
            if (HudMenuAdapter.isAbout(position)) {
                startActivity(new Intent(this, AboutActivity.class));
                return;
            }
            HudMenuAdapter.Action action = HudMenuAdapter.actionAt(position);
            if (action == null) {
                return;
            }
            switch (action) {
                case ZOOM_IN:
                    view.zoomIn();
                    persistCurrentSession();
                    refreshStatusOverlay();
                    break;
                case ZOOM_OUT:
                    view.zoomOut();
                    persistCurrentSession();
                    refreshStatusOverlay();
                    break;
                case RESET:
                    performFullReset();
                    break;
                case FORMULA:
                    openFormulaPicker();
                    break;
                case PALETTE:
                    openPalettePicker();
                    break;
                default:
                    break;
            }
        });
        dialog.setContentView(sheet);
        dialog.show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (view != null) {
            outState.putBundle(STATE_VIEWPORT, view.captureSession().toBundle());
        }
        if (statusOverlay != null) {
            outState.putBoolean(
                    STATE_OVERLAY_VISIBLE,
                    statusOverlay.getVisibility() == View.VISIBLE);
        }
    }

    private void restoreSessionState(Bundle savedInstanceState) {
        if (view == null) {
            return;
        }
        ViewportSession session;
        if (savedInstanceState != null) {
            session = ViewportSession.fromBundle(savedInstanceState.getBundle(STATE_VIEWPORT));
            setStatusOverlayVisible(savedInstanceState.getBoolean(STATE_OVERLAY_VISIBLE, true));
        } else {
            session = SessionStore.load(this);
        }
        if (session == null) {
            return;
        }
        applySession(session);
    }

    private void applySession(ViewportSession session) {
        view.restoreSession(session);
        operatorIndex = Math.max(
                0, Math.min(session.operatorIndex, FormulaCatalog.size() - 1));
        paletteIndex = Math.max(
                0, Math.min(session.paletteIndex, PaletteCatalog.size() - 1));
    }

    private void persistCurrentSession() {
        if (view == null) {
            return;
        }
        ViewportSession session = view.captureSession();
        SessionStore.save(this, session);
        operatorIndex = session.operatorIndex;
        paletteIndex = session.paletteIndex;
    }

    /**
     * Light (white) status/nav icons on the dark fractal (issue #14).
     * appearanceLight* = false means light-colored icons.
     */
    private void applyLightSystemBarIcons() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }

    /**
     * Fractal draws under system bars; pad HUD above the nav bar and keep
     * the render progress hairline below the status icons (issue #14).
     */
    private void applyWindowInsets(
            View hudBar, View progressBar, View statusOverlay, View statusChip) {
        final int hudLeft = hudBar.getPaddingLeft();
        final int hudTop = hudBar.getPaddingTop();
        final int hudRight = hudBar.getPaddingRight();
        final int hudBottom = hudBar.getPaddingBottom();
        final int overlayStart = statusOverlay.getPaddingLeft();
        final int overlayTop = statusOverlay.getPaddingTop();
        final int overlayEnd = statusOverlay.getPaddingRight();
        final int overlayBottom = statusOverlay.getPaddingBottom();
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
            float margin = 8f * getResources().getDisplayMetrics().density;
            statusOverlay.setTranslationX(margin);
            statusOverlay.setTranslationY(statusTop + margin);
            statusOverlay.setPadding(overlayStart, overlayTop, overlayEnd, overlayBottom);
            statusChip.setTranslationX(margin);
            statusChip.setTranslationY(statusTop + margin);
            return insets;
        });
        root.requestApplyInsets();
    }

    private void setStatusOverlayVisible(boolean visible) {
        if (statusOverlay == null || statusOverlayChip == null) {
            return;
        }
        statusOverlay.setVisibility(visible ? View.VISIBLE : View.GONE);
        statusOverlayChip.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    private void refreshStatusOverlay() {
        if (statusOverlay == null) {
            return;
        }
        String[] formulas = FormulaCatalog.labels();
        String[] palettes = PaletteCatalog.labels();
        int safeFormula = Math.max(0, Math.min(operatorIndex, formulas.length - 1));
        int safePalette = Math.max(0, Math.min(paletteIndex, palettes.length - 1));
        int smoothRes = view.isSmooth()
                ? R.string.status_overlay_smooth_on
                : R.string.status_overlay_smooth_off;
        statusOverlay.setText(
                formulas[safeFormula]
                        + "\n"
                        + palettes[safePalette]
                        + "\n"
                        + getString(smoothRes)
                        + "\n"
                        + getString(overlayAlgorithmRes())
                        + "\n"
                        + getString(R.string.status_overlay_iterations, view.effectiveMaxIter()));
    }

    private int overlayAlgorithmRes() {
        IterationSettings settings = view.getIterationSettings();
        IterationSettings.Mode mode = settings != null
                ? settings.mode
                : IterationSettings.Mode.FIXED;
        switch (mode) {
            case SCALE_WITH_ZOOM:
                return R.string.status_overlay_algo_zoom;
            case ADAPTIVE:
                return R.string.status_overlay_algo_adaptive;
            case FIXED:
            default:
                return R.string.status_overlay_algo_fixed;
        }
    }

    private void onRenderBusy(boolean busy) {
        if (busy) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if (renderProgress == null) {
            return;
        }
        renderProgress.removeCallbacks(showRenderProgress);
        if (busy) {
            refreshStatusOverlay();
            renderProgress.setIndeterminate(false);
            renderProgress.setProgress(0);
            renderProgress.postDelayed(showRenderProgress, RENDER_PROGRESS_SHOW_DELAY_MS);
        } else {
            refreshStatusOverlay();
            renderProgress.setIndeterminate(false);
            renderProgress.setVisibility(View.GONE);
        }
    }

    private void onRenderProgress(int completed, int total) {
        if (renderProgress == null || renderProgress.isIndeterminate()) {
            return;
        }
        int max = Math.max(total, 1);
        if (renderProgress.getMax() != max) {
            renderProgress.setMax(max);
        }
        renderProgress.setProgress(Math.min(completed, max));
    }

    /** Issue #31: Adaptive border refine has no fixed sample budget. */
    private void onRenderIndeterminate(boolean indeterminate) {
        if (renderProgress == null) {
            return;
        }
        if (indeterminate) {
            renderProgress.removeCallbacks(showRenderProgress);
            renderProgress.setIndeterminate(true);
            renderProgress.setVisibility(View.VISIBLE);
        } else {
            renderProgress.setIndeterminate(false);
        }
    }

    private void showCoordinateReadout(double real, double imag) {
        dismissCoordinateReadout();
        View root = findViewById(R.id.mandelbrot_root);
        String message = getString(
                R.string.coord_readout,
                formatCoordinate(real),
                formatCoordinate(imag));
        // Indefinite: stays while the finger is down; dismissed on lift.
        coordinateSnackbar = Snackbar.make(root, message, Snackbar.LENGTH_INDEFINITE);
        coordinateSnackbar.show();
    }

    private void dismissCoordinateReadout() {
        if (coordinateSnackbar != null) {
            coordinateSnackbar.dismiss();
            coordinateSnackbar = null;
        }
    }

    private static String formatCoordinate(double value) {
        return String.format(Locale.US, "%.10g", value);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatusOverlay();
        if (resumeInterruptedRender) {
            resumeInterruptedRender = false;
            view.start();
        }
    }

    @Override
    protected void onPause() {
        resumeInterruptedRender = view.isRenderBusy();
        if (resumeInterruptedRender) {
            view.stop();
            onRenderBusy(false);
        }
        persistCurrentSession();
        super.onPause();
    }

    private void openFormulaPicker() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_picker, null);
        TextView title = sheet.findViewById(R.id.picker_title);
        ListView list = sheet.findViewById(R.id.picker_list);
        title.setText(R.string.select_formula);
        FormulaPickerAdapter adapter = new FormulaPickerAdapter(this, list);
        list.setAdapter(adapter);
        int safeChecked = Math.max(0, Math.min(operatorIndex, FormulaCatalog.size() - 1));
        list.setItemChecked(safeChecked, true);
        list.setSelection(safeChecked);
        list.setOnItemClickListener((parent, view1, position, id) -> {
            operatorIndex = position;
            view.setOper(FormulaCatalog.get(position));
            view.reset();
            persistCurrentSession();
            refreshStatusOverlay();
            dialog.dismiss();
        });
        dialog.setContentView(sheet);
        dialog.show();
    }

    private void openPalettePicker() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_picker, null);
        TextView title = sheet.findViewById(R.id.picker_title);
        ListView list = sheet.findViewById(R.id.picker_list);
        title.setText(R.string.select_palette);
        PalettePickerAdapter adapter = new PalettePickerAdapter(this, list);
        list.setAdapter(adapter);
        int safeChecked = Math.max(0, Math.min(paletteIndex, PaletteCatalog.size() - 1));
        list.setItemChecked(safeChecked, true);
        list.setSelection(safeChecked);
        list.setOnItemClickListener((parent, view1, position, id) -> {
            paletteIndex = position;
            view.setPalette(PaletteCatalog.get(position));
            view.start();
            persistCurrentSession();
            refreshStatusOverlay();
            dialog.dismiss();
        });
        dialog.setContentView(sheet);
        dialog.show();
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

    private void openExportSheet() {
        Bitmap bitmap = view.captureDisplayedViewport();
        if (bitmap == null) {
            showExportSnackbar(R.string.export_not_ready);
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_picker, null);
        TextView title = sheet.findViewById(R.id.picker_title);
        ListView list = sheet.findViewById(R.id.picker_list);
        title.setText(R.string.export_title);
        String[] options = {
                getString(R.string.export_share),
                getString(R.string.export_save_gallery),
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                options);
        list.setAdapter(adapter);
        list.setChoiceMode(ListView.CHOICE_MODE_NONE);
        final boolean[] handled = {false};
        list.setOnItemClickListener((parent, row, position, id) -> {
            handled[0] = true;
            dialog.dismiss();
            if (position == 0) {
                shareViewport(bitmap);
            } else {
                saveViewport(bitmap);
            }
        });
        dialog.setOnDismissListener(d -> {
            if (!handled[0] && pendingSaveBitmap != bitmap) {
                bitmap.recycle();
            }
        });
        dialog.setContentView(sheet);
        dialog.show();
    }

    private void shareViewport(Bitmap bitmap) {
        try {
            if (!ViewportPngExporter.share(this, bitmap, getString(R.string.export_share))) {
                showExportSnackbar(R.string.export_share_failed);
            }
        } finally {
            bitmap.recycle();
        }
    }

    private void saveViewport(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                if (ViewportPngExporter.saveToGallery(this, bitmap)) {
                    showExportSnackbar(R.string.export_saved);
                } else {
                    showExportSnackbar(R.string.export_save_failed);
                }
            } finally {
                bitmap.recycle();
            }
        } else {
            pendingSaveBitmap = bitmap;
            saveDocumentLauncher.launch(ViewportPngExporter.defaultFileName());
        }
    }

    private void onLegacySaveResult(Uri uri) {
        Bitmap bitmap = pendingSaveBitmap;
        pendingSaveBitmap = null;
        if (bitmap == null) {
            return;
        }
        try {
            if (uri != null && ViewportPngExporter.writeBitmap(this, bitmap, uri)) {
                showExportSnackbar(R.string.export_saved);
            } else if (uri != null) {
                showExportSnackbar(R.string.export_save_failed);
            }
        } finally {
            bitmap.recycle();
        }
    }

    private void showExportSnackbar(int messageRes) {
        View root = findViewById(R.id.mandelbrot_root);
        Snackbar.make(root, messageRes, Snackbar.LENGTH_SHORT).show();
    }

    private interface PickerSelectionListener {
        void onSelected(int index);
    }
}
