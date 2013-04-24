package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.style.ITrackStyle;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author hiralv
 */
public class Strand extends ColorProvider {
	private static String FORWARD_COLOR = "+";
	private static String REVERSE_COLOR = "-";
	private static Color DEFAULT_FORWARD_COLOR = new Color(204, 255, 255);
	private static Color DEFAULT_REVERSE_COLOR = new Color(51, 255, 255);		
			
	private static Map<String, Class<?>> PARAMETERS = new HashMap<String, Class<?>>();
	static {
		PARAMETERS.put(FORWARD_COLOR, Color.class);
		PARAMETERS.put(REVERSE_COLOR, Color.class);
	}
	
	private Color forwardColor = DEFAULT_FORWARD_COLOR;
	private Color reverseColor = DEFAULT_REVERSE_COLOR;
		
	@Override
	public Color getColor(Object obj) {
		if(obj == Boolean.TRUE){
			return forwardColor;
		}
		return reverseColor;
	}

	public void setForwardColor(Color color){
		forwardColor = color;
	}
	
	public Color getForwardColor(){
		return forwardColor;
	}
	
	public void setReverseColor(Color color){
		reverseColor = color;
	}
	
	public Color getReverseColor(){
		return reverseColor;
	}
		
	@Override
	public Map<String, Class<?>> getParameters(){
		return PARAMETERS;
	}

	@Override
	public void setParameters(Map<String, Object> params){
		for(Entry<String, Object> param : params.entrySet()){
			setParameter(param.getKey(), param.getValue());
		}
	}

	@Override
	public boolean setParameter(String key, Object value) {
		if (FORWARD_COLOR.equals(key) && value instanceof Color) {
			forwardColor = (Color) value;
			return true;
		} else if (REVERSE_COLOR.equals(key) && value instanceof Color) {
			reverseColor = (Color) value;
			return true;
		}
		return false;
	}
	
}
