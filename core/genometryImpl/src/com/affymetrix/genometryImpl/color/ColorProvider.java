package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.awt.Color;
import java.util.Map;

/**
 * A helper class to be used when color is to extracted for each object.
 * @author hiralv
 */
public abstract class ColorProvider {
	
	/**
	 * Get color for the given object
	 * @param sym
	 * @return 
	 */
	public abstract Color getColor(SeqSymmetry sym);
		
	public Map<String, Class<?>> getParameters(){
		return null;
	}

	public final void setParameters(Map<String, Object> params){
		for(Map.Entry<String, Object> param : params.entrySet()){
			setParameter(param.getKey(), param.getValue());
		}
	}
	
	public boolean setParameter(String key, Object value) {
		return false;
	}

	public Object getParameterValue(String key) {
		return null;
	}
	
}
