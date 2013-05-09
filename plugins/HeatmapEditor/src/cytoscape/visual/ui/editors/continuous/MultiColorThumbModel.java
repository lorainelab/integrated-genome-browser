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
		return getVirtualMinimum() + (getFraction(position) * getVirtualRange());
	}
	
	public float getFraction(float position){
		return position / getRange();
	}
	
	public float getPosition(float value){
		float position = value - getVirtualMinimum();
		position /= getVirtualRange();
		position *= getRange();
		return position;
	}
	
	float getRange(){
		return this.getMaximumValue() - this.getMinimumValue();
	}
	
	float getVirtualRange(){
		return this.getVirtualMaximum() - this.getVirtualMinimum();
	}
		
}
