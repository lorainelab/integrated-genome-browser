package com.affymetrix.igb.colorproviders;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.color.ColorProvider;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genoviz.color.ColorPalette;
import com.affymetrix.genoviz.color.ColorScheme;

/**
 *
 * @author hiralv
 */
public class Property extends ColorProvider{
	private final static String PROPERTY = "property";
	public final static String DEFAULT_PROPERTY = "id";
	
	private final static Map<String, Class<?>> PARAMETERS = new HashMap<String, Class<?>>();
	static {
		PARAMETERS.put(PROPERTY, String.class);
	}
	
	protected String property = DEFAULT_PROPERTY;
	ColorPalette cp = new ColorPalette(ColorScheme.ACCENT8);
	
	@Override
	public Color getColor(SeqSymmetry sym){
		if(sym instanceof SymWithProps){
			Object value = ((SymWithProps)sym).getProperty(property);
			if(value != null){
				return cp.getColor(value.toString());
			}
		}
		return null;
	}
	
	@Override
	public Map<String, Class<?>> getParametersType(){
		return PARAMETERS;
	}

	@Override
	public boolean setParameterValue(String key, Object value){
		if(PROPERTY.equals(key) && value instanceof String){
			property = (String)value;
			return true;
		} 
		return false;
	}
	
	@Override
	public Object getParameterValue(String key) {
		if(PROPERTY.equals(key)){
			return property;
		}
		return null;
	}
}
