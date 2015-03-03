package com.affymetrix.genometry.style;

import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class HeatMapExtended extends HeatMap {

    private float[] values;
    private Color[] rangeColors;

    public HeatMapExtended(String name, Color[] colors, float[] values, Color[] rangeColors) {
        super(name, colors);
        this.values = values;
        this.rangeColors = rangeColors;
    }

    public float[] getValues() {
        return values;
    }

    public Color[] getRangeColors() {
        return rangeColors;
    }
}
