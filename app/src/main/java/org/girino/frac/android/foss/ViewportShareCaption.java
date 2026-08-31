package org.girino.frac.android.foss;

import java.util.Locale;

/** Share-sheet caption for exported viewport PNGs (attribution + reproduce hints). */
public final class ViewportShareCaption {

    static final String GITHUB_URL = "https://github.com/girino/android-mandelbrot-set";
    static final String ZAPSTORE_URL = "https://zapstore.dev/apps/org.girino.frac.android.foss";

    private ViewportShareCaption() {
    }

    public static String build(
            String appVersion,
            String formulaLabel,
            double centerX,
            double centerY,
            double scale,
            int operatorIndex,
            double juliaCRe,
            double juliaCIm,
            double phoenixPRe,
            double phoenixPIm) {
        String version = appVersion != null && !appVersion.isEmpty() ? appVersion : "?";
        String formula = formulaLabel != null && !formulaLabel.isEmpty() ? formulaLabel : "Mandelbrot Set";
        StringBuilder out = new StringBuilder();
        out.append("Generated with Fractals by Girino FOSS v").append(version).append('\n');
        out.append("GitHub: ").append(GITHUB_URL).append('\n');
        out.append("Zapstore: ").append(ZAPSTORE_URL).append('\n');
        out.append('\n');
        out.append("Formula: ").append(formula).append('\n');
        out.append("Center: ")
                .append(formatComponent(centerX))
                .append(", ")
                .append(formatComponent(centerY))
                .append('\n');
        out.append("Scale: ").append(formatComponent(scale));
        if (FormulaParamsActivity.usesCParam(operatorIndex)) {
            out.append('\n')
                    .append("Julia c: ")
                    .append(formatComponent(juliaCRe))
                    .append(", ")
                    .append(formatComponent(juliaCIm));
        }
        if (FormulaParamsActivity.usesPParam(operatorIndex)) {
            out.append('\n')
                    .append("Phoenix p: ")
                    .append(formatComponent(phoenixPRe))
                    .append(", ")
                    .append(formatComponent(phoenixPIm));
        }
        return out.toString();
    }

    static String formatComponent(double value) {
        return String.format(Locale.US, "%.10g", value);
    }
}
