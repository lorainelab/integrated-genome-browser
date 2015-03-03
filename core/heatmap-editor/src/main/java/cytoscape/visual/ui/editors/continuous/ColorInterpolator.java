package cytoscape.visual.ui.editors.continuous;

import com.google.common.base.Optional;
import java.awt.Color;

public abstract class ColorInterpolator {

    private final VirtualRange virtualRange;

    protected ColorInterpolator(VirtualRange virtualRange) {
        this.virtualRange = virtualRange;
        Optional<Integer> possible = Optional.of(5);
    }

    public Color[] getColorRange(int range) {
        Color[] colors = new Color[range];
        float factor = (virtualRange.getVirtualMaximum() - virtualRange.getVirtualMinimum()) / range;
        int i = 0;
        for (float p = virtualRange.getVirtualMinimum(); p < virtualRange.getVirtualMaximum(); p += factor) {
            colors[i++] = getColor(p);
        }
//		int i=1;
//		for(float p=virtualRange.getVirtualMinimum()+factor; p<virtualRange.getVirtualMaximum(); p+=factor){
//			colors[i++] = getColor(p);
//		}
//		colors[0] = virtualRange.getBelowColor();
//		colors[range-1] = virtualRange.getAboveColor();
        return colors;
    }

    public Color getColor(float position) {
        float[] positions = virtualRange.getVirtualValues();
        Color[] colors = virtualRange.getColors();

        if (position < positions[1]) {
            return virtualRange.getBelowColor();
        }

        for (int i = 1; i < positions.length - 1; i++) {
            if (Float.compare(position, positions[i]) == 0) {
                return colors[i];
            }
            if (Float.compare(position, positions[i - 1]) == 0) {
                return colors[i - 1];
            }
            if (position < positions[i] && position > positions[i - 1]) {
                return getRangeValue(positions[i - 1], colors[i - 1],
                        positions[i], colors[i], position);
            }
        }

        return virtualRange.getAboveColor();
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
