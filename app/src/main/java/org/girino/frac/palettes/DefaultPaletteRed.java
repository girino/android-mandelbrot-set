package org.girino.frac.palettes;

public class DefaultPaletteRed implements PaletteProvider {

	public static final double gammaCorrection = 0.9;
	
	public int getColor(double value) {
		if (value < epsilon || (1.0-value) < epsilon) {
			return Argb.BLACK;
		}
		int ratio = (int) (Math.pow(value, gammaCorrection) * 255);
		int ratio2 = (int) (Math.pow(value, gammaCorrection/2.0) * 255);
		int ival = (int)(value*255);
		return Argb.rgb(ratio2, ival, ratio);
	}

}
