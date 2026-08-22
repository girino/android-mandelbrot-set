package org.girino.frac.android.foss;

import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import org.girino.frac.viewport.ViewportTransforms;

/**
 * Injects a two-finger pinch (span change) plus midpoint drag into a {@link View}.
 * Used by headless Robolectric tests — no device or emulator required.
 */
final class PinchDragMotionSimulator {
    private final long downTime;
    private long eventTime;

    PinchDragMotionSimulator() {
        downTime = SystemClock.uptimeMillis();
        eventTime = downTime;
    }

    void pinchDown(View view, float midX, float midY, float span) {
        float x0 = midX - span * 0.5f;
        float x1 = midX + span * 0.5f;
        dispatch(view, singleTouch(MotionEvent.ACTION_DOWN, x0, midY));
        dispatch(
                view,
                twoTouch(
                        MotionEvent.ACTION_POINTER_DOWN
                                | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                        x0,
                        midY,
                        x1,
                        midY));
    }

    void pinchMove(View view, float midX, float midY, float span) {
        float x0 = midX - span * 0.5f;
        float x1 = midX + span * 0.5f;
        tick();
        dispatch(view, twoTouch(MotionEvent.ACTION_MOVE, x0, midY, x1, midY));
    }

    void pinchUp(View view, float midX, float midY, float span) {
        float x0 = midX - span * 0.5f;
        float x1 = midX + span * 0.5f;
        dispatch(
                view,
                twoTouch(
                        MotionEvent.ACTION_POINTER_UP
                                | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                        x0,
                        midY,
                        x1,
                        midY));
        tick();
        dispatch(view, singleTouch(MotionEvent.ACTION_UP, x0, midY));
    }

    void panOnly(View view, float startX, float startY, float totalDx, float totalDy, int steps) {
        touchDown(view, startX, startY);
        for (int step = 1; step <= steps; step++) {
            float t = step / (float) steps;
            tick();
            touchMove(view, startX + totalDx * t, startY + totalDy * t);
        }
        tick();
        touchUp(view, startX + totalDx, startY + totalDy);
    }

    void touchDown(View view, float x, float y) {
        dispatch(view, singleTouch(MotionEvent.ACTION_DOWN, x, y));
    }

    void touchMove(View view, float x, float y) {
        tick();
        dispatch(view, singleTouch(MotionEvent.ACTION_MOVE, x, y));
    }

    void touchUp(View view, float x, float y) {
        tick();
        dispatch(view, singleTouch(MotionEvent.ACTION_UP, x, y));
    }

    void panThenPointerDown(
            View view,
            float panStartX,
            float panStartY,
            float panTotalDx,
            float panTotalDy,
            int panSteps,
            float midX,
            float midY,
            float span) {
        dispatch(view, singleTouch(MotionEvent.ACTION_DOWN, panStartX, panStartY));
        for (int step = 1; step <= panSteps; step++) {
            float t = step / (float) panSteps;
            tick();
            dispatch(
                    view,
                    singleTouch(
                            MotionEvent.ACTION_MOVE,
                            panStartX + panTotalDx * t,
                            panStartY + panTotalDy * t));
        }
        float x0 = midX - span * 0.5f;
        float x1 = midX + span * 0.5f;
        float panEndY = panStartY + panTotalDy;
        tick();
        dispatch(
                view,
                twoTouch(
                        MotionEvent.ACTION_POINTER_DOWN
                                | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                        panStartX + panTotalDx,
                        panEndY,
                        x1,
                        panEndY));
    }

    /** One-finger pan, then two-finger pinch (pan must survive {@code beginPinchSession}). */
    void panThenPinch(
            View view,
            float panStartX,
            float panStartY,
            float panTotalDx,
            float panTotalDy,
            int panSteps,
            float secondFingerX,
            float fingerY,
            float spanStart,
            float spanEnd,
            int pinchSteps) {
        dispatch(view, singleTouch(MotionEvent.ACTION_DOWN, panStartX, panStartY));
        for (int step = 1; step <= panSteps; step++) {
            float t = step / (float) panSteps;
            tick();
            dispatch(
                    view,
                    singleTouch(
                            MotionEvent.ACTION_MOVE,
                            panStartX + panTotalDx * t,
                            panStartY + panTotalDy * t));
        }
        float panEndX = panStartX + panTotalDx;
        float panEndY = panStartY + panTotalDy;
        tick();
        dispatch(
                view,
                twoTouch(
                        MotionEvent.ACTION_POINTER_DOWN
                                | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                        panEndX,
                        panEndY,
                        secondFingerX,
                        fingerY));
        float pivotX = (panEndX + secondFingerX) * 0.5f;
        for (int step = 1; step <= pinchSteps; step++) {
            float t = step / (float) pinchSteps;
            float span = spanStart + (spanEnd - spanStart) * t;
            float nx0 = pivotX - span * 0.5f;
            float nx1 = pivotX + span * 0.5f;
            tick();
            dispatch(view, twoTouch(MotionEvent.ACTION_MOVE, nx0, panEndY, nx1, panEndY));
        }
        float nx0End = pivotX - spanEnd * 0.5f;
        float nx1End = pivotX + spanEnd * 0.5f;
        dispatch(
                view,
                twoTouch(
                        MotionEvent.ACTION_POINTER_UP
                                | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                        nx0End,
                        panEndY,
                        nx1End,
                        panEndY));
        tick();
        dispatch(view, singleTouch(MotionEvent.ACTION_UP, nx0End, panEndY));
    }

    static ViewportTransforms.State expectedPanThenPinchCommit(
            ViewportTransforms.State start,
            float panTotalX,
            float panTotalY,
            float pivotX,
            float pivotY,
            float spanStart,
            float spanEnd,
            int pinchSteps,
            int width,
            int height) {
        ViewportTransforms.State afterPan = ViewportTransforms.commitPan(start, panTotalX, panTotalY);
        float positionX = 0f;
        float positionY = 0f;
        double accumulatedScale = 1.0;
        float lastSpan = spanStart;

        for (int step = 1; step <= pinchSteps; step++) {
            float t = step / (float) pinchSteps;
            float span = spanStart + (spanEnd - spanStart) * t;
            accumulatedScale *= span / lastSpan;
            lastSpan = span;
        }

        return ViewportTransforms.commitPinch(
                afterPan,
                accumulatedScale,
                pivotX,
                pivotY,
                width,
                height,
                positionX,
                positionY);
    }

    /** Pinch around {@code mid}, changing span and moving the midpoint by {@code drag*}. */
    void pinchDrag(
            View view,
            float midX,
            float midY,
            float spanStart,
            float spanEnd,
            float dragX,
            float dragY,
            int moveSteps) {
        pinchDown(view, midX, midY, spanStart);
        for (int step = 1; step <= moveSteps; step++) {
            float t = step / (float) moveSteps;
            float span = spanStart + (spanEnd - spanStart) * t;
            float midStepX = midX + dragX * t;
            float midStepY = midY + dragY * t;
            pinchMove(view, midStepX, midStepY, span);
        }
        pinchUp(view, midX + dragX, midY + dragY, spanEnd);
    }

    /** Expected {@link ViewportTransforms#commitPinch} after the same incremental steps as the view. */
    static ViewportTransforms.State expectedCommit(
            ViewportTransforms.State pinchStart,
            float pivotX,
            float pivotY,
            float spanStart,
            float spanEnd,
            float dragX,
            float dragY,
            int moveSteps,
            int width,
            int height) {
        float positionX = 0f;
        float positionY = 0f;
        double accumulatedScale = 1.0;
        float lastMidX = pivotX;
        float lastMidY = pivotY;
        float lastSpan = spanStart;

        for (int step = 1; step <= moveSteps; step++) {
            float t = step / (float) moveSteps;
            float span = spanStart + (spanEnd - spanStart) * t;
            float midStepX = pivotX + dragX * t;
            float midStepY = pivotY + dragY * t;
            accumulatedScale *= span / lastSpan;
            float dmx = midStepX - lastMidX;
            float dmy = midStepY - lastMidY;
            float panScale = (float) accumulatedScale;
            positionX += dmx / panScale;
            positionY += dmy / panScale;
            lastMidX = midStepX;
            lastMidY = midStepY;
            lastSpan = span;
        }

        return ViewportTransforms.commitPinch(
                pinchStart,
                accumulatedScale,
                pivotX,
                pivotY,
                width,
                height,
                positionX,
                positionY);
    }

    private void tick() {
        eventTime += 16;
    }

    private static void dispatch(View view, MotionEvent event) {
        view.onTouchEvent(event);
        event.recycle();
    }

    private MotionEvent singleTouch(int action, float x, float y) {
        tick();
        return MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
    }

    private MotionEvent twoTouch(int action, float x0, float y0, float x1, float y1) {
        MotionEvent.PointerProperties[] props = new MotionEvent.PointerProperties[2];
        props[0] = new MotionEvent.PointerProperties();
        props[0].id = 0;
        props[0].toolType = MotionEvent.TOOL_TYPE_FINGER;
        props[1] = new MotionEvent.PointerProperties();
        props[1].id = 1;
        props[1].toolType = MotionEvent.TOOL_TYPE_FINGER;

        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[2];
        coords[0] = new MotionEvent.PointerCoords();
        coords[0].x = x0;
        coords[0].y = y0;
        coords[1] = new MotionEvent.PointerCoords();
        coords[1].x = x1;
        coords[1].y = y1;

        return MotionEvent.obtain(
                downTime,
                eventTime,
                action,
                2,
                props,
                coords,
                0,
                0,
                1f,
                1f,
                0,
                0,
                0,
                0);
    }
}
