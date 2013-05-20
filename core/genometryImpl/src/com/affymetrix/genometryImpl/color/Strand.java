package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

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
	
	private final static Map<String, Class<?>> PARAMETERS_TYPE;
	static {
		PARAMETERS_TYPE = new HashMap<String, Class<?>>();
		PARAMETERS_TYPE.put(FORWARD_COLOR, Color.class);
		PARAMETERS_TYPE.put(REVERSE_COLOR, Color.class);
	}
	
	private Map<String, Parameter> PARAMETERS_VALUE;
	public Strand(){
		PARAMETERS_VALUE = new HashMap<String, Parameter>();
		PARAMETERS_VALUE.put(FORWARD_COLOR, new Parameter(DEFAULT_FORWARD_COLOR));
		PARAMETERS_VALUE.put(REVERSE_COLOR, new Parameter(DEFAULT_REVERSE_COLOR));
	}
		
	@Override
	public Color getColor(SeqSymmetry sym) {
		if(sym.getSpan(model.getSelectedSeq()).isForward()){
			return (Color)PARAMETERS_VALUE.get(FORWARD_COLOR).get();
		}
		return (Color)PARAMETERS_VALUE.get(REVERSE_COLOR).get();
	}

	@Override
	public Map<String, Class<?>> getParameters(){
		return PARAMETERS_TYPE;
	}

	@Override
	public boolean setParameter(String key, Object value) {
		for(Map.Entry<String, Parameter> parameter : PARAMETERS_VALUE.entrySet()){
			if(parameter.getKey().equals(key)){
				parameter.getValue().set(value);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public Object getParameterValue(String key) {
		for(Map.Entry<String, Parameter> parameter : PARAMETERS_VALUE.entrySet()){
			if(parameter.getKey().equals(key)){
				return parameter.getValue().get();
			}
		}
		return null;
	}
}
