package org.girino.frac.palettes;

public class DefaultPalette implements PaletteProvider {

	public static final double gammaCorrection = 0.99;
	
	public int getColor(double value) {
		if (value < epsilon || (1.0-value) < epsilon) {
			return Argb.BLACK;
		}
		float ratio = (float) Math.pow(value, gammaCorrection);
		return Argb.rgb((int)((1f - ratio)*255), (int)(ratio*255), (int)(value*255));
	}

}
