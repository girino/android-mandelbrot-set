package org.girino.frac.android.foss;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
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

/**
 * Fractal viewport — v1.0.0 gestures with two fixes.
 * 1. Commit is deferred: centerX and scale always describe the published bitmap;
 *    gesture deltas stay as a frozen canvas preview and are folded into a pending
 *    target on full release. The preview is cleared only when the matching bitmap
 *    publishes (atomic handoff, no flash of the stale bitmap).
 * 2. Premature onScaleEnd (finger proximity) is ignored — commit happens
 *    only when the last finger leaves the screen.
 */
public class MandelbrotView extends View {
    private static final int INVALID_POINTER_ID = -1;
    /** Discrete zoom step for HUD / menu / double-tap (issue #6). */
    private static final double ZOOM_STEP = 1.5;

    private final Paint bitmapPaint = new Paint(Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor();
    private final AtomicInteger renderGeneration = new AtomicInteger();

    private volatile Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    private Future<?> renderTask;
    private FractalOperator operator = new OptimizedMandelbrotOperator();
    private PaletteProvider palette = new HSBPalette();
    private boolean smooth;

    private int width = 320;
    private int height = 480;
    /** Viewport of the currently displayed bitmap. */
    private double centerX;
    private double centerY;
    private double scale = 100.0 * 300.0 / width;

    /** Frozen gesture deltas drawn over the stale bitmap (preview). */
    private float accumulatedScale = 1f;
    private float positionX;
    private float positionY;
    /**
     * Live pinch focus (midpoint of the fingers). Preview pans so the bitmap
     * pixel under startFocus stays under this point — content walks with the
     * fingers. Edge gaps during the stale-bitmap preview are acceptable;
     * the new published render always fills the window.
     */
    private float focusX;
    private float focusY;
    /** Focus at gesture start (or last continuous restart); invariant bitmap pixel. */
    private float startFocusX;
    private float startFocusY;
    /** Render target computed at full release; becomes published at first publish. */
    private double targetCenterX;
    private double targetCenterY;
    private double targetScale;
    private boolean hasPendingTarget;
    /** Set by double-tap zoom so the following ACTION_UP does not cancel it. */
    private boolean skipNextCommit;

    private float lastTouchX;
    private float lastTouchY;
    private int activePointerId = INVALID_POINTER_ID;
    private int activePointers;

    public MandelbrotView(Context context) {
        this(context, null);
    }

    public MandelbrotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(0xff0a0a0a);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public void setOper(FractalOperator operator) {
        this.operator = operator;
    }

    public void setPalette(PaletteProvider palette) {
        this.palette = palette;
    }

    public void start() {
        stop();
        if (activePointers > 0 || renderExecutor.isShutdown()
                || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        final boolean pending = hasPendingTarget;
        final int generation = renderGeneration.get();
        final int renderWidth = width;
        final int renderHeight = height;
        final double renderScale = pending ? targetScale : scale;
        final double renderCenterX = pending ? targetCenterX : centerX;
        final double renderCenterY = pending ? targetCenterY : centerY;
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
                if (generation != renderGeneration.get() || activePointers > 0) {
                    return;
                }
                // Atomic handoff: swap bitmap and clear the frozen preview together —
                // the new bitmap under identity transform equals the old one under
                // the frozen transform, so no flash is visible.
                bitmap = rendered;
                centerX = renderCenterX;
                centerY = renderCenterY;
                scale = renderScale;
                hasPendingTarget = false;
                accumulatedScale = 1f;
                positionX = 0f;
                positionY = 0f;
                // Focus must return to screen center too: with s=1 the preview
                // is q = p + (focus - startFocus). Leaving a leftover focus
                // delta would re-apply the pinch-drag on top of the new bitmap
                // (content jumps ~2x the preview drag).
                startFocusX = width / 2f;
                startFocusY = height / 2f;
                focusX = startFocusX;
                focusY = startFocusY;
                invalidate();
            });
        }
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
        startFocusX = width / 2f;
        startFocusY = height / 2f;
        focusX = startFocusX;
        focusY = startFocusY;
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        hasPendingTarget = false;
        accumulatedScale = 1f;
        positionX = 0f;
        positionY = 0f;
        start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Affine preview: q = pos + focus + s*(p - startFocus). Same map used
        // by commitAffinePreview so the handoff shows identical content.
        float s = accumulatedScale;
        canvas.save();
        canvas.translate(positionX, positionY);
        canvas.translate(focusX, focusY);
        canvas.scale(s, s);
        canvas.translate(-startFocusX, -startFocusY);
        canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                stop();
                activePointers = 1;
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                startFocusX = width / 2f;
                startFocusY = height / 2f;
                focusX = startFocusX;
                focusY = startFocusY;
                activePointerId = event.getPointerId(0);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                activePointers = Math.max(activePointers, event.getPointerCount());
                break;
            case MotionEvent.ACTION_MOVE:
                int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex >= 0) {
                    float x = event.getX(pointerIndex);
                    float y = event.getY(pointerIndex);
                    if (!scaleDetector.isInProgress()) {
                        positionX += x - lastTouchX;
                        positionY += y - lastTouchY;
                        invalidate();
                    }
                    lastTouchX = x;
                    lastTouchY = y;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                activePointers = Math.max(0, activePointers - 1);
                int releasedIndex = event.getActionIndex();
                if (event.getPointerId(releasedIndex) == activePointerId && event.getPointerCount() > 1) {
                    int newIndex = releasedIndex == 0 ? 1 : 0;
                    lastTouchX = event.getX(newIndex);
                    lastTouchY = event.getY(newIndex);
                    activePointerId = event.getPointerId(newIndex);
                }
                break;
            case MotionEvent.ACTION_UP:
                activePointers = 0;
                if (skipNextCommit) {
                    skipNextCommit = false;
                    activePointerId = INVALID_POINTER_ID;
                    // Double-tap already queued a pending target; kick render
                    // now that no pointers are down (start() gates on that).
                    start();
                } else {
                    commitGestureAndRender();
                }
                performClick();
                break;
            case MotionEvent.ACTION_CANCEL:
                activePointers = 0;
                skipNextCommit = false;
                commitGestureAndRender();
                break;
            default:
                break;
        }
        // After ACTION_DOWN's stop(), so double-tap zoomAt is not cancelled.
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    /**
     * Last finger left the screen: fold the frozen preview into a pending
     * render target. Preview map: q = pos + focus + s*(p - startFocus), so
     * effective translation d = pos + focus - s*startFocus. The pending
     * viewport is chosen so identity draw matches that preview everywhere;
     * the new render fills the whole window (preview edge gaps are OK).
     */
    private void commitGestureAndRender() {
        activePointerId = INVALID_POINTER_ID;
        if (accumulatedScale != 1f || positionX != 0f || positionY != 0f || movedFocus()) {
            float dx = positionX + focusX - accumulatedScale * startFocusX;
            float dy = positionY + focusY - accumulatedScale * startFocusY;
            ViewportTransforms.State committed = ViewportTransforms.commitAffinePreview(
                    new ViewportTransforms.State(centerX, centerY, scale),
                    accumulatedScale,
                    dx,
                    dy,
                    width,
                    height);
            targetCenterX = committed.centerX;
            targetCenterY = committed.centerY;
            targetScale = committed.scale;
            hasPendingTarget = true;
        }
        start();
    }

    private boolean movedFocus() {
        return focusX != startFocusX || focusY != startFocusY;
    }

    private void requestRender(double newScale, double newCenterX, double newCenterY) {
        targetScale = newScale;
        targetCenterX = newCenterX;
        targetCenterY = newCenterY;
        hasPendingTarget = true;
        start();
    }

    /** Zoom in about the screen center (HUD / menu). */
    public void zoomIn() {
        zoomAt(width / 2f, height / 2f, ZOOM_STEP);
    }

    /** Zoom out about the screen center (HUD / menu). */
    public void zoomOut() {
        zoomAt(width / 2f, height / 2f, 1.0 / ZOOM_STEP);
    }

    /**
     * Zoom by factor about a screen point, keeping the complex coordinate
     * under that point fixed. Uses deferred commit (pending target + render).
     */
    public void zoomAt(float screenX, float screenY, double factor) {
        if (factor == 1.0 || width <= 0 || height <= 0) {
            return;
        }
        // Drop any unfinished preview so the discrete zoom starts from the
        // published viewport (buttons / double-tap are not pinch sessions).
        accumulatedScale = 1f;
        positionX = 0f;
        positionY = 0f;
        startFocusX = width / 2f;
        startFocusY = height / 2f;
        focusX = startFocusX;
        focusY = startFocusY;
        ViewportTransforms.State next = ViewportTransforms.commitPinch(
                new ViewportTransforms.State(centerX, centerY, scale),
                factor,
                screenX,
                screenY,
                width,
                height,
                0f,
                0f);
        requestRender(next.scale, next.centerX, next.centerY);
    }

    /** Alias for zoomIn (older call sites). */
    public void zoom() {
        zoomIn();
    }

    /** Toggles continuous (smooth) iteration coloring and re-renders. */
    public void smooth() {
        smooth = !smooth;
        start();
    }

    public boolean isSmooth() {
        return smooth;
    }

    public void reset() {
        requestRender(100.0 * 300.0 / 320.0 * width / 320.0, 0, 0);
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
            float fx = detector.getFocusX();
            float fy = detector.getFocusY();
            // Detector (re)started at a different focus (late begin / extra
            // finger). Shift startFocus so the affine map stays continuous.
            if (fx != focusX || fy != focusY) {
                float inv = 1f / accumulatedScale;
                startFocusX += (fx - focusX) * inv;
                startFocusY += (fy - focusY) * inv;
            }
            focusX = fx;
            focusY = fy;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            accumulatedScale *= detector.getScaleFactor();
            focusX = detector.getFocusX();
            focusY = detector.getFocusY();
            invalidate();
            return true;
        }

        /**
         * Fires early when fingers get close (hardware drops a pointer) and on
         * normal release. Either way the commit is deferred to full release, so
         * this is intentionally a no-op — the detector restarting on a new
         * POINTER_DOWN simply continues accumulating into the frozen preview.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Finger is still down (ACTION_DOWN of the second tap). Clear
            // pointer state so zoomAt → start() is not blocked by the
            // activePointers gate; skip the following UP's gesture commit.
            skipNextCommit = true;
            activePointers = 0;
            activePointerId = INVALID_POINTER_ID;
            zoomAt(e.getX(), e.getY(), ZOOM_STEP);
            return true;
        }
    }

    // --- Robolectric gesture tests (same package) ---

    void testingStopRender() {
        stop();
    }

    double testingCenterX() {
        return centerX;
    }

    double testingCenterY() {
        return centerY;
    }

    double testingScale() {
        return scale;
    }

    float testingAccumulatedScale() {
        return accumulatedScale;
    }

    float testingFocusX() {
        return focusX;
    }

    float testingFocusY() {
        return focusY;
    }

    float testingStartFocusX() {
        return startFocusX;
    }

    float testingStartFocusY() {
        return startFocusY;
    }

    float testingPositionX() {
        return positionX;
    }

    float testingPositionY() {
        return positionY;
    }

    int testingActivePointers() {
        return activePointers;
    }

    boolean testingHasPendingTarget() {
        return hasPendingTarget;
    }

    double testingTargetScale() {
        return targetScale;
    }

    double testingTargetCenterX() {
        return targetCenterX;
    }

    double testingTargetCenterY() {
        return targetCenterY;
    }

    int testingRenderGeneration() {
        return renderGeneration.get();
    }

    /** Publish gate: stale generations and mid-gesture steps must not swap the bitmap. */
    boolean testingWouldPublishBitmap(int generation) {
        return generation == renderGeneration.get() && activePointers == 0;
    }

    /**
     * Complex coordinate currently displayed at screen (x, y) by the live
     * preview — inverse of the onDraw affine transform.
     */
    double testingPreviewComplexX(float x, float y) {
        float s = accumulatedScale;
        float bitmapX = (x - positionX - focusX) / s + startFocusX;
        return org.girino.frac.viewport.ViewportTransforms.complexX(
                bitmapX, width, centerX, scale);
    }

    double testingPreviewComplexY(float x, float y) {
        float s = accumulatedScale;
        float bitmapY = (y - positionY - focusY) / s + startFocusY;
        return org.girino.frac.viewport.ViewportTransforms.complexY(
                bitmapY, height, centerY, scale);
    }

    // --- test-only state manipulation ---

    void testingSimulatePointersDown() {
        activePointers = 1;
    }

    void testingSimulateAllPointersUp() {
        activePointers = 0;
    }

    /**
     * Applies the same preview clear as the atomic handoff on bitmap publish.
     * Used to assert no residual focus delta doubles the committed drag.
     */
    void testingSimulateAtomicHandoffClear() {
        accumulatedScale = 1f;
        positionX = 0f;
        positionY = 0f;
        startFocusX = width / 2f;
        startFocusY = height / 2f;
        focusX = startFocusX;
        focusY = startFocusY;
    }

    /** Promote pending target to published viewport (tests discrete zoom chains). */
    void testingApplyPendingAsPublished() {
        if (!hasPendingTarget) {
            return;
        }
        centerX = targetCenterX;
        centerY = targetCenterY;
        scale = targetScale;
        hasPendingTarget = false;
        testingSimulateAtomicHandoffClear();
    }

    /** Mirrors onDoubleTap: clear pointers then zoom so start() is not gated. */
    void testingSimulateDoubleTapZoom(float x, float y) {
        skipNextCommit = true;
        activePointers = 0;
        activePointerId = INVALID_POINTER_ID;
        zoomAt(x, y, ZOOM_STEP);
    }
}
