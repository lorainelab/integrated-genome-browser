package com.affymetrix.igb.colorproviders;

import java.awt.Color;

import com.affymetrix.genometryImpl.color.ColorProvider;
import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;

/**
 *
 * @author hiralv
 */
public class Property extends ColorProvider {
	private final static String PROPERTY = "property";
	public final static String DEFAULT_PROPERTY = "id";
	private final static String PROPERTY_VALUE = "value";
	public final static String DEFAULT_PROPERTY_VALUE = "";
	private final static String MATCH_COLOR = "match";
	private final static Color DEFAULT_MATCH_COLOR = Color.GREEN;
	private final static String NOT_MATCH_COLOR = "not_match";
	private final static Color DEFAULT_NOT_MATCH_COLOR = Color.RED;
	private Float float_property_value = null;
		
	protected Parameter<String> property = new Parameter<String>(DEFAULT_PROPERTY) {
		@Override
		public boolean set(Object e){
			String prop = (String)e;
			if(prop == null || prop.length() == 0){
				return false;
			}
			super.set(e);
			return true;
		}
	};
	
	protected Parameter<String> property_value = new Parameter<String>(DEFAULT_PROPERTY_VALUE){
		@Override
		public boolean set(Object e){
			try {
				float_property_value = Float.parseFloat(e.toString());
			} catch(Exception ex) {
			}
			return super.set(e);
		}
	};
	
	private Parameter<Color> matchColor = new Parameter<Color>(DEFAULT_MATCH_COLOR);
	private Parameter<Color> notMatchColor = new Parameter<Color>(DEFAULT_NOT_MATCH_COLOR);
	
//	private ColorPalette cp = new ColorPalette(ColorScheme.ACCENT8);
	
	public Property(){
		super();
		parameters.addParameter(PROPERTY, String.class, property);
		parameters.addParameter(PROPERTY_VALUE, String.class, property_value);
		parameters.addParameter(MATCH_COLOR, Color.class, matchColor);
		parameters.addParameter(NOT_MATCH_COLOR, Color.class, notMatchColor);
	}
	
	@Override
	public Color getColor(SeqSymmetry sym){
		if(sym instanceof SymWithProps){
			Object value = ((SymWithProps)sym).getProperty(property.get());
			if((value instanceof Float || value instanceof Double) 
					&& float_property_value != null && Float.compare(float_property_value, (Float)value) == 0) {
				return matchColor.get();
			} else if(value != null && property_value.get() != null && value.toString().equals(property_value.get())){
				return matchColor.get();
//				return cp.getColor(value.toString());
			}
		}
		return notMatchColor.get();
	}
}
