package org.girino.frac.viewport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Regression for GitHub issue #2: pinch zoom must keep the complex point under
 * the gesture focus stable (no abrupt viewport jump).
 */
class ViewportTransformsTest {

    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1920;
    private static final double EPS = 1e-12;

    @Test
    void complexMappingMatchesRendererConvention() {
        assertEquals(
                0.25,
                ViewportTransforms.complexX(WIDTH / 2.0 + 50, WIDTH, 0.0, 200.0),
                EPS);
        assertEquals(
                -0.1,
                ViewportTransforms.complexY(HEIGHT / 2.0 - 20, HEIGHT, 0.0, 200.0),
                EPS);
    }

    @Test
    void panKeepsDraggedScreenPointFixed() {
        ViewportTransforms.State before = new ViewportTransforms.State(-0.5, 0.2, 150.0);
        float panX = 80f;
        float panY = -40f;
        float screenX = 300f;
        float screenY = 900f;

        // With identity scale preview, screen S shows bitmap S - pan.
        double complexBefore = ViewportTransforms.complexX(
                screenX - panX, WIDTH, before.centerX, before.scale);
        ViewportTransforms.State after = ViewportTransforms.commitPan(before, panX, panY);
        double complexAfter =
                ViewportTransforms.complexX(screenX, WIDTH, after.centerX, after.scale);

        assertEquals(complexBefore, complexAfter, EPS);
        assertEquals(
                ViewportTransforms.complexY(screenY - panY, HEIGHT, before.centerY, before.scale),
                ViewportTransforms.complexY(screenY, HEIGHT, after.centerY, after.scale),
                EPS);
    }

    @ParameterizedTest
    @CsvSource({
            // focusX, focusY, factor, panX, panY, centerX, centerY, scale
            "540, 960, 2.0, 0, 0, 0.0, 0.0, 100.0",
            "100, 200, 1.5, 0, 0, -0.75, 0.1, 180.0",
            "900, 1600, 0.5, 0, 0, 0.2, -0.3, 220.0",
            "200, 300, 2.5, 40, -25, -0.1, 0.05, 160.0",
            "800, 100, 0.75, -30, 50, 0.0, 0.0, 120.0"
    })
    void pinchKeepsComplexPointUnderFocusFixed(
            float focusX,
            float focusY,
            float factor,
            float panX,
            float panY,
            double centerX,
            double centerY,
            double scale) {
        ViewportTransforms.State before =
                new ViewportTransforms.State(centerX, centerY, scale);

        // Preview: geometry sees translate(pan) then scale-around-focus.
        // Screen focus F maps to bitmap B = F - pan / factor... wait.
        // Canvas calls: scale(s, F); translate(pan). Applied to geometry: translate then scale.
        // screen = F + s * ((B + pan) - F)
        // At screen = F: B = F - pan
        // So complex under focus uses bitmap coord (focus - pan) at old scale.
        double beforeX = ViewportTransforms.complexX(
                focusX - panX, WIDTH, before.centerX, before.scale);
        double beforeY = ViewportTransforms.complexY(
                focusY - panY, HEIGHT, before.centerY, before.scale);

        ViewportTransforms.State after = ViewportTransforms.commitPinch(
                before, factor, focusX, focusY, WIDTH, HEIGHT, panX, panY);

        double afterX =
                ViewportTransforms.complexX(focusX, WIDTH, after.centerX, after.scale);
        double afterY =
                ViewportTransforms.complexY(focusY, HEIGHT, after.centerY, after.scale);

        assertEquals(beforeX, afterX, EPS, "real under focus jumped");
        assertEquals(beforeY, afterY, EPS, "imag under focus jumped");
        assertEquals(scale * factor, after.scale, EPS);
    }

    @Test
    void factorOneWithPanOnlyCommitsPan() {
        ViewportTransforms.State before = new ViewportTransforms.State(0.1, -0.2, 90.0);
        ViewportTransforms.State after =
                ViewportTransforms.commitPinch(before, 1f, 10f, 20f, WIDTH, HEIGHT, 15f, -5f);
        ViewportTransforms.State panOnly = ViewportTransforms.commitPan(before, 15f, -5f);

        assertEquals(panOnly.centerX, after.centerX, EPS);
        assertEquals(panOnly.centerY, after.centerY, EPS);
        assertEquals(panOnly.scale, after.scale, EPS);
    }

    @Test
    void oldCenterZoomWouldMoveCornerFocus_regressionGuard() {
        // Documents the bug from issue #2: scaling scale only (center zoom) moves the
        // complex point under a corner focus; commitPinch must not.
        ViewportTransforms.State before = new ViewportTransforms.State(0.0, 0.0, 100.0);
        float focusX = 50f;
        float focusY = 50f;
        float factor = 2f;

        double underFocusBefore =
                ViewportTransforms.complexX(focusX, WIDTH, before.centerX, before.scale);

        // Buggy approach: scale *= factor, keep center.
        double buggyAfter =
                ViewportTransforms.complexX(focusX, WIDTH, before.centerX, before.scale * factor);
        assertEquals(underFocusBefore / factor, buggyAfter, EPS);

        ViewportTransforms.State fixed = ViewportTransforms.commitPinch(
                before, factor, focusX, focusY, WIDTH, HEIGHT, 0f, 0f);
        assertEquals(
                underFocusBefore,
                ViewportTransforms.complexX(focusX, WIDTH, fixed.centerX, fixed.scale),
                EPS);
    }
}
