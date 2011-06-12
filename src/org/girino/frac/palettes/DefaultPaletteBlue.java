package org.girino.frac.palettes;

import android.graphics.Color;

public class DefaultPaletteBlue implements PaletteProvider {

	public static final double gammaCorrection = 0.6;
	
	public int getColor(double value) {
		if (value < epsilon || (1.0-value) < epsilon) {
			return Color.BLACK;
		}
		float ratio = (float) Math.pow(value, gammaCorrection);
		return Color.rgb(0, 0, (int)(ratio*255));
	}

}
