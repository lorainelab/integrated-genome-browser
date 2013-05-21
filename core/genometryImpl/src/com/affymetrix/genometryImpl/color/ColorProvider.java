package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.awt.Color;
import java.util.Map;

/**
 * A helper class to be used when color is to extracted for each object.
 * @author hiralv
 */
public abstract class ColorProvider implements IParameters {
	
	/**
	 * Get color for the given object
	 * @param sym
	 * @return 
	 */
	public abstract Color getColor(SeqSymmetry sym);
	
	@Override
	public Map<String, Class<?>> getParametersType(){
		return null;
	}

	@Override
	public final void setParametersValue(Map<String, Object> params){
		for(Map.Entry<String, Object> param : params.entrySet()){
			setParameterValue(param.getKey(), param.getValue());
		}
	}
	
	@Override
	public boolean setParameterValue(String key, Object value) {
		return false;
	}

	@Override
	public Object getParameterValue(String key) {
		return null;
	}
	
}
