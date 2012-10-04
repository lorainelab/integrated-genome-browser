package com.affymetrix.genometryImpl.style;

import java.awt.Color;


public class DynamicStyleHeatMap extends HeatMap {
	private final ITrackStyle style;
	private final float lowPercent;
	private final float highPercent;
	private Color saveBackgroundColor;
	private Color saveForegroundColor;
	public DynamicStyleHeatMap(String name, ITrackStyle style, float lowPercent, float highPercent) {
		super(name, new Color[256]);
		this.style = style;
		this.lowPercent = lowPercent;
		this.highPercent = highPercent;
		checkColors();
	}

	@Override
	public Color[] getColors() {
		checkColors();
		return super.getColors();
	}

	@Override
	public Color getColor(int heatmap_index) {
		checkColors();
		return super.getColor(heatmap_index);
	}

	private boolean compareColors(Color color1, Color color2) {
		if (color1 == null && color2 == null) {
			return true;
		}
		if (color1 == null || color2 == null) {
			return false;
		}
		return color1.equals(color2);
	}

	private void resetColors(Color low, Color high) {
		for (int i=0; i<256; i++) {
			float x = (i*1.0f)/255.0f;
			colors[i] = interpolateColor(low, high, x);
		}
		
	}

	private void checkColors() {
		if (style != null && (!compareColors(saveBackgroundColor, style.getBackground()) || !compareColors(saveForegroundColor, style.getForeground()))) {
			if (style.getBackground() == null || style.getForeground() == null) {
				resetColors(Color.black, Color.white);
			}
			else {
				saveBackgroundColor = style.getBackground();
				saveForegroundColor = style.getForeground();
				resetColors(interpolateColor(saveBackgroundColor, saveForegroundColor, lowPercent), interpolateColor(saveBackgroundColor, saveForegroundColor, highPercent));
			}
		}
	}
}
