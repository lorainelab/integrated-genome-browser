package com.affymetrix.genometryImpl.color;

import java.awt.Color;
import java.util.Map;

/**
 * A helper interface to be used when color is to extracted for each object.
 * @author hiralv
 */
public interface ColorProvider {
	
	/**
	 * Get color for the given object
	 * @param obj
	 * @return 
	 */
	public Color getColor(Object obj);
	
	public void update();
	
	public Map<String, Class<?>> getParameters();

	public void setParameters( Map<String, Object> params);
	
	public boolean setParameter(String key, Object value);
	
}
