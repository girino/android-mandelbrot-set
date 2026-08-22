package org.girino.frac.android.foss;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.girino.frac.operators.Complex;
import org.girino.frac.operators.FractalOperator;
import org.girino.frac.operators.OptimizedMandelbrotOperator;
import org.girino.frac.palettes.HSBPalette;
import org.girino.frac.palettes.PaletteProvider;
import org.girino.frac.viewport.ViewportTransforms;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class MandelbrotView extends View {
    private static final int INVALID_POINTER_ID = -1;

    private final Paint bitmapPaint = new Paint(Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final ScaleGestureDetector scaleDetector;
    private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor();
    private final AtomicInteger renderGeneration = new AtomicInteger();

    private volatile Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    private Future<?> renderTask;
    private FractalOperator operator = new OptimizedMandelbrotOperator();
    private PaletteProvider palette = new HSBPalette();
    private boolean smooth;

    private int width = 320;
    private int height = 480;
    private double centerX;
    private double centerY;
    private double scale = 100.0 * 300.0 / width;

    /** Live gesture scale factor (1 while idle). */
    private float accumulatedScale = 1f;
    private float focusX;
    private float focusY;
    private float positionX;
    private float positionY;
    private float lastTouchX;
    private float lastTouchY;
    private int activePointerId = INVALID_POINTER_ID;

    /**
     * Preview transform kept until the first progressive frame of the matching
     * render arrives, so the bitmap does not snap back before the new fractal.
     */
    private float previewScale = 1f;
    private float previewFocusX;
    private float previewFocusY;
    private float previewPosX;
    private float previewPosY;
    private boolean previewResetPending;
    /** Model snapshot before an unpublished gesture commit (for gesture restart). */
    private double preCommitCenterX;
    private double preCommitCenterY;
    private double preCommitScale;
    private boolean hasPreCommit;

    public MandelbrotView(Context context) {
        super(context);
        setBackgroundColor(0xff0a0a0a);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        focusX = width / 2f;
        focusY = height / 2f;
        previewFocusX = focusX;
        previewFocusY = focusY;
    }

    public void setOper(FractalOperator operator) {
        this.operator = operator;
    }

    public void setPalette(PaletteProvider palette) {
        this.palette = palette;
    }

    public void start() {
        stop();
        if (getWidth() <= 0 || getHeight() <= 0 || renderExecutor.isShutdown()) {
            return;
        }

        final int generation = renderGeneration.get();
        final int renderWidth = width;
        final int renderHeight = height;
        final double renderScale = scale;
        final double renderCenterX = centerX;
        final double renderCenterY = centerY;
        final FractalOperator renderOperator = operator;
        final PaletteProvider renderPalette = palette;
        final boolean renderSmooth = smooth;

        renderTask = renderExecutor.submit(() -> render(
                generation,
                renderWidth,
                renderHeight,
                renderScale,
                renderCenterX,
                renderCenterY,
                renderOperator,
                renderPalette,
                renderSmooth));
    }

    public void stop() {
        renderGeneration.incrementAndGet();
        if (renderTask != null) {
            renderTask.cancel(true);
            renderTask = null;
        }
    }

    private void render(
            int generation,
            int renderWidth,
            int renderHeight,
            double renderScale,
            double renderCenterX,
            double renderCenterY,
            FractalOperator renderOperator,
            PaletteProvider renderPalette,
            boolean renderSmooth) {
        Bitmap rendered = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888);
        Canvas renderCanvas = new Canvas(rendered);
        Paint renderPaint = new Paint(Paint.DITHER_FLAG);
        renderCanvas.drawColor(0xff0a0a0a);
        Complex point = new Complex();

        for (int step = 8; step > 0; step /= 2) {
            for (int y = 0; y < renderHeight; y += step) {
                for (int x = 0; x < renderWidth; x += step) {
                    if (generation != renderGeneration.get() || Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    point.set(
                            (x - renderWidth / 2.0) / renderScale + renderCenterX,
                            (y - renderHeight / 2.0) / renderScale + renderCenterY);
                    double value = renderOperator.apply(point, 40, renderSmooth);
                    renderPaint.setColor(renderPalette.getColor(value));
                    if (step == 1) {
                        renderCanvas.drawPoint(x, y, renderPaint);
                    } else {
                        renderCanvas.drawRect(x, y, x + step, y + step, renderPaint);
                    }
                }
            }
            post(() -> {
                if (generation == renderGeneration.get()) {
                    bitmap = rendered;
                    if (previewResetPending) {
                        clearPreviewTransform();
                        previewResetPending = false;
                        hasPreCommit = false;
                    }
                    invalidate();
                }
            });
        }
    }

    private void syncPreviewFromGesture() {
        previewScale = accumulatedScale;
        previewFocusX = focusX;
        previewFocusY = focusY;
        previewPosX = positionX;
        previewPosY = positionY;
    }

    private void clearPreviewTransform() {
        previewScale = 1f;
        previewPosX = 0f;
        previewPosY = 0f;
        accumulatedScale = 1f;
        positionX = 0f;
        positionY = 0f;
    }

    private void rememberPreCommitIfNeeded() {
        if (!hasPreCommit) {
            preCommitCenterX = centerX;
            preCommitCenterY = centerY;
            preCommitScale = scale;
            hasPreCommit = true;
        }
    }

    private void restorePreCommitIfNeeded() {
        if (!hasPreCommit) {
            return;
        }
        centerX = preCommitCenterX;
        centerY = preCommitCenterY;
        scale = preCommitScale;
        hasPreCommit = false;
        previewResetPending = false;
        accumulatedScale = previewScale;
        positionX = previewPosX;
        positionY = previewPosY;
    }

    private boolean hasUnpublishedPreview() {
        return hasPreCommit || previewScale != 1f || previewPosX != 0f || previewPosY != 0f;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        stop();
        if (w <= 0 || h <= 0) {
            return;
        }
        scale *= w / (double) width;
        width = w;
        height = h;
        focusX = width / 2f;
        focusY = height / 2f;
        previewFocusX = focusX;
        previewFocusY = focusY;
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        // Geometry sees translate first, then scale around focus (canvas applies in reverse).
        canvas.scale(previewScale, previewScale, previewFocusX, previewFocusY);
        canvas.translate(previewPosX, previewPosY);
        canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                activePointerId = event.getPointerId(0);
                return true;
            case MotionEvent.ACTION_MOVE:
                int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex < 0) {
                    return true;
                }
                float x = event.getX(pointerIndex);
                float y = event.getY(pointerIndex);
                if (event.getPointerCount() == 1 && !scaleDetector.isInProgress()) {
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;
                    positionX += dx;
                    positionY += dy;
                    if (previewResetPending) {
                        // Keep frozen pinch preview; only slide it with the new pan.
                        previewPosX += dx;
                        previewPosY += dy;
                    } else {
                        syncPreviewFromGesture();
                    }
                    invalidate();
                }
                lastTouchX = x;
                lastTouchY = y;
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                int releasedIndex = event.getActionIndex();
                if (event.getPointerId(releasedIndex) == activePointerId && event.getPointerCount() > 1) {
                    int newIndex = releasedIndex == 0 ? 1 : 0;
                    lastTouchX = event.getX(newIndex);
                    lastTouchY = event.getY(newIndex);
                    activePointerId = event.getPointerId(newIndex);
                }
                return true;
            case MotionEvent.ACTION_UP:
                applyTranslation();
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                applyTranslation();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    /**
     * Commits outstanding pan into the fractal center. No-ops when scale commit
     * already folded the same pan (live position counters were cleared there).
     */
    private void applyTranslation() {
        activePointerId = INVALID_POINTER_ID;
        if (positionX == 0f && positionY == 0f) {
            return;
        }
        rememberPreCommitIfNeeded();
        ViewportTransforms.State next = ViewportTransforms.commitPan(
                new ViewportTransforms.State(centerX, centerY, scale),
                positionX,
                positionY);
        centerX = next.centerX;
        centerY = next.centerY;
        if (!previewResetPending) {
            // Freeze pan preview before clearing live counters.
            syncPreviewFromGesture();
        }
        // If previewResetPending, previewPos was already updated during MOVE.
        positionX = 0f;
        positionY = 0f;
        previewResetPending = true;
        start();
    }

    /**
     * Commits pinch zoom around the gesture focus, folding any outstanding pan
     * at the pre-zoom scale. Keeps the preview transform until the new bitmap.
     */
    private void applyScale() {
        float factor = accumulatedScale;
        if (factor == 1f) {
            return;
        }
        rememberPreCommitIfNeeded();
        ViewportTransforms.State next = ViewportTransforms.commitPinch(
                new ViewportTransforms.State(centerX, centerY, scale),
                factor,
                focusX,
                focusY,
                width,
                height,
                positionX,
                positionY);
        centerX = next.centerX;
        centerY = next.centerY;
        scale = next.scale;
        syncPreviewFromGesture();
        accumulatedScale = 1f;
        positionX = 0f;
        positionY = 0f;
        previewResetPending = true;
        start();
    }

    public void zoom() {
        scale *= 1.5;
        start();
    }

    public void smooth() {
        smooth = !smooth;
        start();
    }

    public void reset() {
        centerX = 0;
        centerY = 0;
        scale = 100.0 * 300.0 / 320.0 * width / 320.0;
        clearPreviewTransform();
        previewResetPending = false;
        hasPreCommit = false;
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        renderExecutor.shutdownNow();
        super.onDetachedFromWindow();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            stop();
            if (hasPreCommit) {
                // Roll back unpublished model commit and resume from the frozen preview.
                restorePreCommitIfNeeded();
            } else if (!hasUnpublishedPreview()) {
                accumulatedScale = 1f;
            } else {
                // Preview still reflects live pan/scale without a model commit yet.
                accumulatedScale = previewScale;
                positionX = previewPosX;
                positionY = previewPosY;
            }
            focusX = detector.getFocusX();
            focusY = detector.getFocusY();
            syncPreviewFromGesture();
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            accumulatedScale *= detector.getScaleFactor();
            focusX = detector.getFocusX();
            focusY = detector.getFocusY();
            syncPreviewFromGesture();
            invalidate();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            focusX = detector.getFocusX();
            focusY = detector.getFocusY();
            applyScale();
        }
    }
}
