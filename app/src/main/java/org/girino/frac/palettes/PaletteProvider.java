package org.girino.frac.palettes;

import android.graphics.Color;


public interface PaletteProvider {
	public static final double epsilon = 0.0000000001;
	public int getColor(double value);
}
