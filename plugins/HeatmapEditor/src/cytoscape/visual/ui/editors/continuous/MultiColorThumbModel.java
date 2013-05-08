package cytoscape.visual.ui.editors.continuous;

import java.awt.Color;
import java.util.List;

import org.jdesktop.swingx.multislider.DefaultMultiThumbModel;
import org.jdesktop.swingx.multislider.Thumb;

public class MultiColorThumbModel extends DefaultMultiThumbModel<Color> {
	public static final Color DEFAULT_BELOW_COLOR = Color.black;
	public static final Color DEFAULT_ABOVE_COLOR = Color.white;
	public static final float DEFAULT_MIN_VIRTUAL = 0.0f;
	public static final float DEFAULT_MAX_VIRTUAL = 100.0f;
	
	private Color belowColor, aboveColor;
	private float minVirtualValue, maxVirtualValue;
	
	public MultiColorThumbModel(){
		this.minVirtualValue = DEFAULT_MIN_VIRTUAL;
		this.maxVirtualValue = DEFAULT_MAX_VIRTUAL;
		this.belowColor = DEFAULT_BELOW_COLOR;
		this.aboveColor = DEFAULT_ABOVE_COLOR;
	}
	
	public MultiColorThumbModel(float minValue, float maxValue, Color below, Color above){
		this.minVirtualValue = minValue;
		this.maxVirtualValue = maxValue;
		this.belowColor = below;
		this.aboveColor = above;
	}
	
	public void setBelowColor(Color color){
		this.belowColor = color;
	}
	
	public Color getBelowColor(){
		return belowColor;
	}
	
	public void setAboveColor(Color color){
		this.aboveColor = color;
	}
	
	public Color getAboveColor(){
		return aboveColor;
	}
	
	public void setVirtualMinimum(float minValue){
		this.minVirtualValue = minValue;
	}
	
	public float getVirtualMinimum(){
		return minVirtualValue;
	}
	
	public void setVirtualMaximum(float maxValue){
		this.maxVirtualValue = maxValue;
	}
	
	public float getVirtualMaximum(){
		return maxVirtualValue;
	}
	
	public float getVirtualValue(float position){
		float range = maxVirtualValue - minVirtualValue;
		return minVirtualValue + (getFraction(position) * range);
	}
	
	public float getFraction(float position){
		float range = maxVirtualValue - minVirtualValue;
		//float minFraction = (float)minValue/(float)range;
		return (position / range);
	}
	
	
	
	public Color getColor(float position) {
		if (this.getThumbCount() > 0) {
			List<Thumb<Color>> sortedThumbs = getSortedThumbs();
			for (int i = 1; i < sortedThumbs.size(); i++) {
				Thumb<Color> lowerThumb = sortedThumbs.get(i-1);
				Thumb<Color> upperThumb = sortedThumbs.get(i);
				if (upperThumb.getPosition() > position && position > lowerThumb.getPosition()) {
					return getRangeValue(lowerThumb.getPosition(), lowerThumb.getObject(), 
							upperThumb.getPosition(), upperThumb.getObject(), position);
				}
			}
			if (position <= getThumbAt(0).getPosition()) {
				return belowColor;
			} else {
				return aboveColor;
			}
		}
		return Color.black;
	}
	
	private Color getRangeValue(float lowerDomain, Color lowerRange,
			float upperDomain, Color upperRange, float domainValue) {
		if (lowerDomain == upperDomain)
			return lowerRange;

		double frac = (domainValue - lowerDomain) / (upperDomain - lowerDomain);

		return getRangeValue(frac, lowerRange, upperRange);
	}
	
	private Color getRangeValue(double frac, Color lowerColor, Color upperColor) {

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
