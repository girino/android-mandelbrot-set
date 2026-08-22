package org.girino.frac.android.foss;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
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
    private static final String TAG = "MandelbrotView";

    private final Paint bitmapPaint = new Paint(Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final boolean debugViewport;
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
    /** Pinch pivot and model snapshot captured at onScaleBegin (must stay fixed). */
    private float pinchPivotX;
    private float pinchPivotY;
    private double pinchStartCenterX;
    private double pinchStartCenterY;
    private double pinchStartScale;
    /** Pan offset in bitmap space (screen delta divided by current preview scale). */
    private float positionX;
    private float positionY;
    private float lastTouchX;
    private float lastTouchY;
    private int activePointerId = INVALID_POINTER_ID;

    /** Canvas preview until the matching render finishes. */
    private float previewScale = 1f;
    private float previewFocusX;
    private float previewFocusY;
    private float previewPosX;
    private float previewPosY;

    /** Finger down / drag in progress (defer bitmap swaps until release). */
    private boolean panInProgress;
    /** Two-finger pinch active for this touch sequence. */
    private boolean pinchSessionStarted;
    /** Pinch ended (one finger lifted) but not yet committed on ACTION_UP. */
    private boolean pinchNeedsCommit;
    /** Last span between pointers for manual pinch tracking. */
    private float lastPinchSpan = -1f;
    /** Latest progressive frame waiting while a gesture is active. */
    private volatile Bitmap deferredBitmap;

    /** Committed by gesture but applied to center/scale only when the matching bitmap publishes. */
    private boolean hasPendingViewport;
    private double pendingCenterX;
    private double pendingCenterY;
    private double pendingScale;
    /** Generation that must publish before pending viewport is discarded. */
    private int pendingViewportGeneration = -1;

    public MandelbrotView(Context context) {
        super(context);
        debugViewport =
                (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        setBackgroundColor(0xff0a0a0a);
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
        final double renderScale = renderTargetScale();
        final double renderCenterX = renderTargetCenterX();
        final double renderCenterY = renderTargetCenterY();
        final FractalOperator renderOperator = operator;
        final PaletteProvider renderPalette = palette;
        final boolean renderSmooth = smooth;

        if (hasPendingViewport) {
            pendingViewportGeneration = generation;
        }

        debugViewport(
                "start gen=" + generation
                        + " renderCenter=(" + renderCenterX + "," + renderCenterY + ")"
                        + " renderScale=" + renderScale
                        + " bitmapCenter=(" + centerX + "," + centerY + ")"
                        + " bitmapScale=" + scale
                        + " pending=" + hasPendingViewport);

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
        clearDeferredPublish();
        if (renderTask != null) {
            renderTask.cancel(true);
            renderTask = null;
        }
    }

    private double renderTargetCenterX() {
        return hasPendingViewport ? pendingCenterX : centerX;
    }

    private double renderTargetCenterY() {
        return hasPendingViewport ? pendingCenterY : centerY;
    }

    private double renderTargetScale() {
        return hasPendingViewport ? pendingScale : scale;
    }

    private ViewportTransforms.State effectiveViewportState() {
        return new ViewportTransforms.State(
                renderTargetCenterX(), renderTargetCenterY(), renderTargetScale());
    }

    private void setPendingViewport(ViewportTransforms.State state) {
        pendingCenterX = state.centerX;
        pendingCenterY = state.centerY;
        pendingScale = state.scale;
        hasPendingViewport = true;
        pendingViewportGeneration = -1;
    }

    private void applyPendingViewport() {
        if (!hasPendingViewport) {
            return;
        }
        centerX = pendingCenterX;
        centerY = pendingCenterY;
        scale = pendingScale;
        clearPendingViewport();
    }

    private void clearPendingViewport() {
        hasPendingViewport = false;
        pendingViewportGeneration = -1;
    }

    private void debugViewport(String message) {
        if (debugViewport) {
            Log.d(
                    TAG,
                    message
                            + " preview(s=" + previewScale
                            + " pos=" + previewPosX + "," + previewPosY
                            + " focus=" + previewFocusX + "," + previewFocusY + ")"
                            + " gesture=" + isGestureActive()
                            + " committed=(" + centerX + "," + centerY + ") s=" + scale
                            + " pending=" + hasPendingViewport);
        }
    }

    /** Logs complex coords at screen center: preview-on-bitmap vs target viewport (detects publish jumps). */
    private void debugJumpCheck(String phase, double targetCenterX, double targetCenterY, double targetScale) {
        if (!debugViewport || !hasLivePreview()) {
            return;
        }
        float screenCx = width * 0.5f;
        float screenCy = height * 0.5f;
        float bitmapX = (screenCx - previewFocusX) / previewScale + previewFocusX - previewPosX;
        float bitmapY = (screenCy - previewFocusY) / previewScale + previewFocusY - previewPosY;
        double previewComplexX =
                ViewportTransforms.complexX(bitmapX, width, centerX, scale);
        double previewComplexY =
                ViewportTransforms.complexY(bitmapY, height, centerY, scale);
        double targetComplexX =
                ViewportTransforms.complexX(screenCx, width, targetCenterX, targetScale);
        double targetComplexY =
                ViewportTransforms.complexY(screenCy, height, targetCenterY, targetScale);
        double dx = previewComplexX - targetComplexX;
        double dy = previewComplexY - targetComplexY;
        if (Math.abs(dx) > 1e-6 || Math.abs(dy) > 1e-6) {
            Log.w(
                    TAG,
                    phase
                            + " JUMP? previewComplex=("
                            + previewComplexX
                            + ","
                            + previewComplexY
                            + ") target=("
                            + targetComplexX
                            + ","
                            + targetComplexY
                            + ") delta=("
                            + dx
                            + ","
                            + dy
                            + ")");
        } else {
            Log.d(TAG, phase + " jumpCheck ok delta=(" + dx + "," + dy + ")");
        }
    }

    private boolean isGestureActive() {
        return panInProgress || pinchSessionStarted || pinchNeedsCommit;
    }

    private static float pinchSpan(MotionEvent event) {
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float) Math.hypot(dx, dy);
    }

    private static float pinchMidX(MotionEvent event) {
        return (event.getX(0) + event.getX(1)) * 0.5f;
    }

    private static float pinchMidY(MotionEvent event) {
        return (event.getY(0) + event.getY(1)) * 0.5f;
    }

    private void beginPinchSession(MotionEvent event) {
        stop();
        pinchNeedsCommit = false;
        if (hasLivePreview()) {
            accumulatedScale = previewScale;
            positionX = previewPosX;
            positionY = previewPosY;
        } else {
            accumulatedScale = 1f;
        }
        pinchPivotX = pinchMidX(event);
        pinchPivotY = pinchMidY(event);
        pinchStartCenterX = renderTargetCenterX();
        pinchStartCenterY = renderTargetCenterY();
        pinchStartScale = renderTargetScale();
        focusX = pinchPivotX;
        focusY = pinchPivotY;
        lastPinchSpan = pinchSpan(event);
        syncPreviewFromGesture();
    }

    private void updatePinch(MotionEvent event) {
        float span = pinchSpan(event);
        if (lastPinchSpan > 0f) {
            accumulatedScale *= span / lastPinchSpan;
            focusX = pinchPivotX;
            focusY = pinchPivotY;
            syncPreviewFromGesture();
            invalidate();
        }
        lastPinchSpan = span;
    }

    private void clearDeferredPublish() {
        deferredBitmap = null;
    }

    private void publishRenderFrame(Bitmap rendered, int generation, int step) {
        if (isGestureActive()) {
            deferredBitmap = rendered;
            debugViewport("publish deferred gen=" + generation + " step=" + step);
            invalidate();
            return;
        }
        if (hasPendingViewport && generation != pendingViewportGeneration) {
            debugViewport(
                    "publish skip stale gen=" + generation + " pendingGen=" + pendingViewportGeneration);
            return;
        }
        if (hasPendingViewport) {
            debugJumpCheck(
                    "publish gen=" + generation + " step=" + step,
                    pendingCenterX,
                    pendingCenterY,
                    pendingScale);
        }
        bitmap = rendered;
        applyPendingViewport();
        clearPreviewTransform();
        deferredBitmap = null;
        debugViewport("publish applied gen=" + generation + " step=" + step);
        invalidate();
    }

    /** Ends touch handling: commit viewport, then start one render with gesture flags already cleared. */
    private void finishTouchGesture() {
        final boolean commitPinch = pinchSessionStarted || pinchNeedsCommit;

        pinchSessionStarted = false;
        pinchNeedsCommit = false;
        panInProgress = false;
        lastPinchSpan = -1f;
        activePointerId = INVALID_POINTER_ID;

        clearDeferredPublish();

        boolean viewportChanged = false;
        if (commitPinch) {
            viewportChanged = commitScaleIfNeeded();
        }
        if (commitPanIfNeeded()) {
            viewportChanged = true;
        }
        if (viewportChanged) {
            start();
        }
        debugViewport("finishTouchGesture changed=" + viewportChanged);
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
            final int publishStep = step;
            post(() -> {
                if (generation != renderGeneration.get()) {
                    return;
                }
                publishRenderFrame(rendered, generation, publishStep);
            });
        }
    }

    private float previewScaleForPan() {
        float s = previewScale;
        return s != 0f ? s : 1f;
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

    private boolean hasLivePreview() {
        return previewScale != 1f || previewPosX != 0f || previewPosY != 0f;
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
        clearPreviewTransform();
        clearPendingViewport();
        start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0xff0a0a0a);
        canvas.save();
        canvas.scale(previewScale, previewScale, previewFocusX, previewFocusY);
        canvas.translate(previewPosX, previewPosY);
        canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                panInProgress = true;
                pinchSessionStarted = false;
                pinchNeedsCommit = false;
                lastPinchSpan = -1f;
                clearDeferredPublish();
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                activePointerId = event.getPointerId(0);
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() == 2) {
                    if (!pinchSessionStarted) {
                        beginPinchSession(event);
                        pinchSessionStarted = true;
                    } else {
                        lastPinchSpan = pinchSpan(event);
                    }
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() >= 2 && pinchSessionStarted) {
                    updatePinch(event);
                    return true;
                }
                int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex < 0) {
                    return true;
                }
                float x = event.getX(pointerIndex);
                float y = event.getY(pointerIndex);
                if (event.getPointerCount() == 1) {
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;
                    float panScale = previewScaleForPan();
                    positionX += dx / panScale;
                    positionY += dy / panScale;
                    syncPreviewFromGesture();
                    invalidate();
                }
                lastTouchX = x;
                lastTouchY = y;
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerCount() - 1 == 1 && pinchSessionStarted) {
                    lastPinchSpan = -1f;
                    pinchNeedsCommit = true;
                    focusX = pinchPivotX;
                    focusY = pinchPivotY;
                    syncPreviewFromGesture();
                }
                int releasedIndex = event.getActionIndex();
                if (event.getPointerId(releasedIndex) == activePointerId && event.getPointerCount() > 1) {
                    int newIndex = releasedIndex == 0 ? 1 : 0;
                    lastTouchX = event.getX(newIndex);
                    lastTouchY = event.getY(newIndex);
                    activePointerId = event.getPointerId(newIndex);
                }
                return true;
            case MotionEvent.ACTION_UP:
                finishTouchGesture();
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                finishTouchGesture();
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

    private boolean commitPanIfNeeded() {
        if (positionX == 0f && positionY == 0f) {
            return false;
        }
        ViewportTransforms.State next = ViewportTransforms.commitPan(
                effectiveViewportState(),
                positionX,
                positionY);
        setPendingViewport(next);
        syncPreviewFromGesture();
        positionX = 0f;
        positionY = 0f;
        debugViewport("commitPan pendingCenter=(" + next.centerX + "," + next.centerY + ")");
        return true;
    }

    private boolean commitScaleIfNeeded() {
        double factor = accumulatedScale;
        if (factor == 1.0 && positionX == 0f && positionY == 0f) {
            return false;
        }
        ViewportTransforms.State next = ViewportTransforms.commitPinch(
                new ViewportTransforms.State(pinchStartCenterX, pinchStartCenterY, pinchStartScale),
                factor,
                pinchPivotX,
                pinchPivotY,
                width,
                height,
                positionX,
                positionY);
        setPendingViewport(next);
        syncPreviewFromGesture();
        accumulatedScale = 1f;
        positionX = 0f;
        positionY = 0f;
        debugViewport("commitPinch pendingCenter=(" + next.centerX + "," + next.centerY + ") scale=" + next.scale);
        return true;
    }

    public void zoom() {
        scale *= 1.5;
        clearPreviewTransform();
        clearPendingViewport();
        start();
    }

    public void smooth() {
        smooth = !smooth;
        clearPendingViewport();
        start();
    }

    public void reset() {
        centerX = 0;
        centerY = 0;
        scale = 100.0 * 300.0 / 320.0 * width / 320.0;
        clearPreviewTransform();
        clearPendingViewport();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        renderExecutor.shutdownNow();
        super.onDetachedFromWindow();
    }
}
