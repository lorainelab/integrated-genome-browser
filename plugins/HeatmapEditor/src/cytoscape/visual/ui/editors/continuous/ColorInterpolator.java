package cytoscape.visual.ui.editors.continuous;

import java.awt.Color;


public abstract class ColorInterpolator {

	public Color getColor(float position, float[] positions, Color[] colors) {
		for (int i = 1; i < positions.length; i++) {
			if (positions[i] > position && position > positions[i - 1]) {
				return getRangeValue(positions[i - 1], colors[i - 1], positions[i], colors[i], position);
			}
		}
		if (position <= positions[0]) {
			return colors[0];
		}
		return colors[positions.length - 1];
	}

	private Color getRangeValue(float lowerDomain, Color lowerRange, float upperDomain, Color upperRange, float domainValue) {
		if (lowerDomain == upperDomain) {
			return lowerRange;
		}
		double frac = (domainValue - lowerDomain) / (upperDomain - lowerDomain);
		return getRangeValue(frac, lowerRange, upperRange);
	}
    
	protected abstract Color getRangeValue(double frac, Color lowerColor, Color upperColor);
}
