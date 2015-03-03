package cytoscape.visual.ui.editors.continuous;

import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class BasicVirtualRange implements VirtualRange {

    private Color colors[];
    private float values[];

    public BasicVirtualRange(float[] values, Color[] colors) {
        set(values, colors);
    }

    private void set(float[] values, Color[] colors) {
        if (values.length != colors.length) {
            throw new IllegalArgumentException("Both lengths should be same");
        }
        if (values.length < 2) {
            throw new IllegalArgumentException("Minimum length should be two");
        }

        this.values = values;
        this.colors = colors;
    }

    @Override
    public Color getAboveColor() {
        return colors[colors.length - 1];
    }

    @Override
    public Color getBelowColor() {
        return colors[0];
    }

    @Override
    public float getVirtualMaximum() {
        return values[values.length - 1];
    }

    @Override
    public float getVirtualMinimum() {
        return values[0];
    }

    @Override
    public Color[] getColors() {
        return colors;
    }

    @Override
    public float[] getVirtualValues() {
        return values;
    }

}
