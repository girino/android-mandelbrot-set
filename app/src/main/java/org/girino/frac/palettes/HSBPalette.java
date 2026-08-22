package org.girino.frac.palettes;

public class HSBPalette implements PaletteProvider {

	public static final double gammaCorrection = 0.99;
	
	float[] hsv = new float[] {0, 0.9f, 0.9f};
	public int getColor(double value) {
		if (value < epsilon || (1.0-value) < epsilon) {
			return Argb.BLACK;
		}
		double hue = 0.9 * value;
		hue -= Math.floor(hue);
		hue *= 360;
		hsv[0] = (float)hue;
		return Argb.hsvToColor(hsv);
	}

}
