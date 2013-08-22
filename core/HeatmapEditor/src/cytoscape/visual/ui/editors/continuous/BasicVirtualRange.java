package cytoscape.visual.ui.editors.continuous;

import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class BasicVirtualRange implements VirtualRange {
	
	private Color belowColor, aboveColor;
	private Color colors[];
	private float minVirtualValue, maxVirtualValue;
	private float values[];
	
	public BasicVirtualRange(float[] values, Color[] colors){
		set(values, colors);
	}
	
	private void set(float[] values, Color[] colors){
		if(values.length != colors.length){
			throw new IllegalArgumentException("Both lengths should be same");
		}
		if(values.length < 2){
			throw new IllegalArgumentException("Minimum length should be two");
		}
		
		this.values = values;
		this.colors = colors;
		this.minVirtualValue = values[0];
		this.maxVirtualValue = values[values.length - 1];
		this.belowColor = colors[0];
		this.aboveColor = colors[colors.length - 1];
	}
	
	@Override
	public Color getAboveColor(){
		return aboveColor;
	}

	@Override
	public Color getBelowColor(){
		return belowColor;
	}

	@Override
	public float getVirtualMaximum() {
		return maxVirtualValue;
	}

	@Override
	public float getVirtualMinimum() {
		return minVirtualValue;
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
