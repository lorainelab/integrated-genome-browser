package com.affymetrix.genometryImpl.style;

import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class HeatMapExtended extends HeatMap {
	
	private float[] values;
	private Color[] colors;
	
	public HeatMapExtended(String name, Color[] rangeColors, float[] values, Color[] colors) {
		super(name, rangeColors);
		this.values = values;
		this.colors = colors;
	}
	
	public float[] getValues(){
		return values;
	}
	
	public Color[] getColors(){
		return colors;
	}
}
