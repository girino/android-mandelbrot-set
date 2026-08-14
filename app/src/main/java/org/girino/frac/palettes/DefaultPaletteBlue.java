package org.girino.frac.palettes;

import android.graphics.Color;

public class DefaultPaletteBlue implements PaletteProvider {

	public static final double gammaCorrection = 0.9;
	
	public int getColor(double value) {
		if (value < epsilon || (1.0-value) < epsilon) {
			return Color.BLACK;
		}
		int ratio = (int) (Math.pow(value, gammaCorrection) * 255);
		int ratio2 = (int) (Math.pow(value, gammaCorrection/2.0) * 255);
		int ival = (int)(value*255);
		return Color.rgb(ival, ratio, ratio2);
	}

}
