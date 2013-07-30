package cytoscape.visual.ui.editors.continuous;

import java.awt.Color;

public abstract class ColorInterpolator {
	private final VirtualRange virtualRange;
	
	protected ColorInterpolator(VirtualRange virtualRange){
		this.virtualRange = virtualRange;
	}
	
	public Color[] getColorRange(int range){	
		Color[] colors = new Color[range];
		float factor = (virtualRange.getVirtualMaximum() - virtualRange.getVirtualMinimum())/range;
		for(int i=0; i<range; i++){
			colors[i] = getColor(i*factor);
		}
		return colors;
	}
	
	public Color getColor(float position) {
		float[] positions = virtualRange.getVirtualValues();
		Color[] colors = virtualRange.getColors();
		for (int i = 1; i < positions.length; i++) {
			if (positions[i] > position && position > positions[i - 1]) {
				return getRangeValue(positions[i - 1], colors[i - 1],
						positions[i], colors[i], position);
			}
		}
		if (position <= positions[0]) {
			return virtualRange.getBelowColor();
		}
		return virtualRange.getAboveColor();
	}

	private Color getRangeValue(float lowerDomain, Color lowerRange, float upperDomain, Color upperRange, float domainValue) {
		if (lowerDomain == upperDomain) {
			return lowerRange;
		}
//		This does not work with negative values		
//		double frac = (domainValue - lowerDomain) / (upperDomain - lowerDomain);
//		return getRangeValue(frac, lowerRange, upperRange);
		double frac = (domainValue - Math.abs(lowerDomain)) / (upperDomain - lowerDomain);
		return getRangeValue(Math.abs(frac), lowerRange, upperRange);
	}
    
	protected abstract Color getRangeValue(double frac, Color lowerColor, Color upperColor);
}
