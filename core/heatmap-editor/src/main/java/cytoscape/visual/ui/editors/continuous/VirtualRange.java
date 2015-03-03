package cytoscape.visual.ui.editors.continuous;

import java.awt.Color;

/**
 *
 * @author hiralv
 */
public interface VirtualRange {

    public Color getAboveColor();

    public Color getBelowColor();

    public Color[] getColors();

    public float getVirtualMaximum();

    public float getVirtualMinimum();

    public float[] getVirtualValues();

}
