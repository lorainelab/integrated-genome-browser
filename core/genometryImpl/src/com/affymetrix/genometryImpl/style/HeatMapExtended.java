package com.affymetrix.genometryImpl.style;

import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class HeatMapExtended extends HeatMap {
	
	public static float[] DEFAULT_VALUES = new float[]{1,150,850,1000};
	public static Color[] DEFAULT_COLORS = new Color[]{Color.BLACK, Color.BLACK, Color.WHITE, Color.WHITE};
	
	private float[] values;
	private Color[] rangeColors;
	
	public HeatMapExtended(String name, Color[] colors, float[] values, Color[] rangeColors) {
		super(name, colors);
		this.values = values;
		this.rangeColors = rangeColors;
	}
	
	public float[] getValues(){
		return values;
	}
	
	public Color[] getRangeColors(){
		return rangeColors;
	}
}
