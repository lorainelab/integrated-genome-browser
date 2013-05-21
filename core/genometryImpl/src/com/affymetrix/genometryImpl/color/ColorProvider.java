package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.awt.Color;
import java.util.Map;

/**
 * A helper class to be used when color is to extracted for each object.
 * @author hiralv
 */
public abstract class ColorProvider implements IParameters {
	protected Parameters parameters;
	
	protected ColorProvider(){
		parameters = new Parameters();
	}
	
	/**
	 * Get color for the given object
	 * @param sym
	 * @return 
	 */
	public abstract Color getColor(SeqSymmetry sym);
	
	@Override
	public Map<String, Class<?>> getParametersType(){
		return parameters.getParametersType();
	}

	@Override
	public final void setParametersValue(Map<String, Object> params){
		parameters.setParametersValue(params);
	}
	
	@Override
	public boolean setParameterValue(String key, Object value) {
		return parameters.setParameterValue(key, value);
	}

	@Override
	public Object getParameterValue(String key) {
		return parameters.getParameterValue(key);
	}
	
}
