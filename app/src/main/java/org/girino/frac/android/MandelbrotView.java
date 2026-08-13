package org.girino.frac.android;

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

    private float accumulatedScale = 1f;
    private float positionX;
    private float positionY;
    private float lastTouchX;
    private float lastTouchY;
    private int activePointerId = INVALID_POINTER_ID;

    public MandelbrotView(Context context) {
        super(context);
        setBackgroundColor(0xff0a0a0a);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
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
                    invalidate();
                }
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
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float dx = (1 - accumulatedScale) * width / 2f;
        float dy = (1 - accumulatedScale) * height / 2f;
        canvas.save();
        canvas.translate(positionX + dx, positionY + dy);
        canvas.scale(accumulatedScale, accumulatedScale);
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
                if (!scaleDetector.isInProgress()) {
                    positionX += x - lastTouchX;
                    positionY += y - lastTouchY;
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

    private void applyTranslation() {
        activePointerId = INVALID_POINTER_ID;
        centerX -= positionX / scale;
        centerY -= positionY / scale;
        positionX = 0;
        positionY = 0;
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
            accumulatedScale = 1f;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            accumulatedScale *= detector.getScaleFactor();
            invalidate();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            scale *= accumulatedScale;
            accumulatedScale = 1f;
            start();
        }
    }
}
