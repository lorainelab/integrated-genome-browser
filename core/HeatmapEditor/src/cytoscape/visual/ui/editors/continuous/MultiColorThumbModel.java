package cytoscape.visual.ui.editors.continuous;

import java.awt.Color;

import org.jdesktop.swingx.multislider.DefaultMultiThumbModel;

public class MultiColorThumbModel extends DefaultMultiThumbModel<Color> implements VirtualRange {
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
	
	public MultiColorThumbModel(float[] values, Color[] colors){
		set(values, colors);
	}
	
	public void setBelowColor(Color color){
		this.belowColor = color;
	}
	
	public final void set(float[] values, Color[] colors){
		if(values.length != colors.length){
			throw new IllegalArgumentException("Both lengths should be same");
		}
		if(values.length < 2){
			throw new IllegalArgumentException("Minimum length should be two");
		}
		// Clear previous thumbs
		for(int i=0; i<this.getThumbCount(); i++){
			this.removeThumb(i);
		}
		
		this.minVirtualValue = values[0];
		this.maxVirtualValue = values[values.length - 1];
		this.belowColor = colors[0];
		this.aboveColor = colors[colors.length - 1];
		
		for(int i=1; i<values.length - 1; i++){
			addThumb(getPosition(values[i]), colors[i]);
		}
	}
	
	@Override
	public Color getBelowColor(){
		return belowColor;
	}
	
	public void setAboveColor(Color color){
		this.aboveColor = color;
	}
	
	@Override
	public Color getAboveColor(){
		return aboveColor;
	}
	
	public void setVirtualMinimum(float minValue){
		this.minVirtualValue = minValue;
	}
	
	@Override
	public float getVirtualMinimum(){
		return minVirtualValue;
	}
	
	public void setVirtualMaximum(float maxValue){
		this.maxVirtualValue = maxValue;
	}
	
	@Override
	public float getVirtualMaximum(){
		return maxVirtualValue;
	}
	
	public float getPosition(float value){
		float position = value - getVirtualMinimum();
		position /= getVirtualRange();
		position *= getRange();
		return position;
	}
	
	public float getVirtualValue(float position){
		return getVirtualMinimum() + (getFraction(position) * getVirtualRange());
	}
	
	public float getFraction(float position){
		return position / getRange();
	}
	
	private float getRange(){
		return this.getMaximumValue() - this.getMinimumValue();
	}
	
	private float getVirtualRange(){
		return this.getVirtualMaximum() - this.getVirtualMinimum();
	}
	
	@Override
	public float[] getVirtualValues(){
		float[] values = new float[this.getThumbCount() + 2];
		values[0] = this.getVirtualMinimum();
		for(int i=0; i<this.getThumbCount(); i++){
			values[i+1] = getVirtualValue(this.getThumbAt(i).getPosition());
		}
		values[values.length - 1] = this.getVirtualMaximum();
		return values;
	}
	
	@Override
	public Color[] getColors(){
		Color[] colors = new Color[this.getThumbCount() + 2];
		colors[0] = this.getBelowColor();
		for(int i=0; i<this.getThumbCount(); i++){
			colors[i+1] = this.getThumbAt(i).getObject();
		}
		colors[colors.length - 1] = this.getAboveColor();
		return colors;
	}
}
