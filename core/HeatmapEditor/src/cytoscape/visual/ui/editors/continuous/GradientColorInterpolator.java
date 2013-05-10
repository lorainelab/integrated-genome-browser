
package cytoscape.visual.ui.editors.continuous;

import java.awt.Color;

public class GradientColorInterpolator extends ColorInterpolator {
	
	public GradientColorInterpolator(VirtualRange virtualRange){
		super(virtualRange);
	}
	
	@Override
	protected Color getRangeValue(double frac, Color lowerColor, Color upperColor) {

		double red = lowerColor.getRed()
				+ (frac * (upperColor.getRed() - lowerColor.getRed()));
		double green = lowerColor.getGreen()
				+ (frac * (upperColor.getGreen() - lowerColor.getGreen()));
		double blue = lowerColor.getBlue()
				+ (frac * (upperColor.getBlue() - lowerColor.getBlue()));
		double alpha = lowerColor.getAlpha()
				+ (frac * (upperColor.getAlpha() - lowerColor.getAlpha()));

		return new Color((int) Math.round(red), (int) Math.round(green),
				(int) Math.round(blue), (int) Math.round(alpha));
	}
}
