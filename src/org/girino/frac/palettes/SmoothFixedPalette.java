package org.girino.frac.palettes;

import android.graphics.Color;

public class SmoothFixedPalette implements PaletteProvider {

	protected static int[] colors;
	
	static {
		colors = new int[1024];
		
		for (int i=0;i<1024;i++)
		{
		    int colorValueR=0;
		    int colorValueG=0;
		    int colorValueB=0;
		    if (i >= 768)
		    {
		        colorValueB = i - 768;
		        //colorValueG = i - 768;
		        colorValueR = 255 - colorValueB;
		    }
		    else if (i >= 512)
		    {
		        colorValueR = i - 512;
		        colorValueG = 255 - colorValueR;
		    }
		    else if (i >= 256)
		    {
		        colorValueG = i - 256;
		        colorValueB = 255 - colorValueG;
		    }
		    else
		    {
		        colorValueB = i;
		    }
		    colors[i] = Color.rgb(colorValueR, colorValueG, colorValueB);
		}
	}
	
	
	public int getColor(double value) {
		if (value < epsilon || (1.0-value) < epsilon) {
			return Color.BLACK;
		}
		return colors[(int)(colors.length * value)];
	}

}
