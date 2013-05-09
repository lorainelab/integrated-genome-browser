package cytoscape.visual.ui.editors.continuous;

import java.awt.Color;
import java.util.List;
import org.jdesktop.swingx.multislider.Thumb;


public abstract class ColorInterpolator {
	private final MultiColorThumbModel model;
	
	protected ColorInterpolator(MultiColorThumbModel model){
		this.model = model;
	}
	
	public Color[] getColorRange(int range){	
		Color[] colors = new Color[range];
		float factor = model.getRange()/range;
		for(int i=0; i<range; i++){
			colors[i] = getColor(i*factor);
		}
		return colors;
	}
	
	public Color getColor(float position) {
		if (model.getThumbCount() > 0) {
			List<Thumb<Color>> sortedThumbs = model.getSortedThumbs();
			for (int i = 1; i < sortedThumbs.size(); i++) {
				Thumb<Color> lowerThumb = sortedThumbs.get(i-1);
				Thumb<Color> upperThumb = sortedThumbs.get(i);
				if (upperThumb.getPosition() > position && position > lowerThumb.getPosition()) {
					return getRangeValue(lowerThumb.getPosition(), lowerThumb.getObject(), 
							upperThumb.getPosition(), upperThumb.getObject(), position);
				}
			}
			if (position <= sortedThumbs.get(0).getPosition()) {
				return model.getBelowColor();
			} else {
				return model.getAboveColor();
			}
		}
		return Color.black;
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
