package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author hiralv
 */
public class Strand extends ColorProvider {
	private final static String FORWARD_COLOR = "+";
	private final static String REVERSE_COLOR = "-";
	private final static Color DEFAULT_FORWARD_COLOR = new Color(204, 255, 255);
	private final static Color DEFAULT_REVERSE_COLOR = new Color(51, 255, 255);		
	private static GenometryModel model = GenometryModel.getGenometryModel();
	
	private static Map<String, Class<?>> PARAMETERS = new HashMap<String, Class<?>>();
	static {
		PARAMETERS.put(FORWARD_COLOR, Color.class);
		PARAMETERS.put(REVERSE_COLOR, Color.class);
	}
	
	private Color forwardColor = DEFAULT_FORWARD_COLOR;
	private Color reverseColor = DEFAULT_REVERSE_COLOR;
		
	@Override
	public Color getColor(SeqSymmetry sym) {
		if(sym.getSpan(model.getSelectedSeq()).isForward()){
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
	
	@Override
	public Object getParameterValue(String key) {
		if (FORWARD_COLOR.equals(key)) {
			return forwardColor;
		} else if (REVERSE_COLOR.equals(key)) {
			return reverseColor;
		}
		return null;
	}
}
