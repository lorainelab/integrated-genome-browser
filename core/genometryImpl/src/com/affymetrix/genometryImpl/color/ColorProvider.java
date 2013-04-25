package com.affymetrix.genometryImpl.color;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A helper class to be used when color is to extracted for each object.
 * @author hiralv
 */
public abstract class ColorProvider {
	public final static Map<String, Class<? extends ColorProvider>> OPTIONS;
	static {
		OPTIONS = new LinkedHashMap<String, Class<? extends ColorProvider>>();
		OPTIONS.put("None", null);
		OPTIONS.put("RGB", RGB.class);
		OPTIONS.put("Score", Score.class);
		OPTIONS.put("Strand", Strand.class);
	}
	
	public static ColorProvider getCPInstance(Class<? extends ColorProvider> clazz) {
		try {
			if(clazz != null){
				return clazz.getConstructor().newInstance();
			}
		} catch (Exception ex) {
			
		}
		return null;
	}
	
	/**
	 * Get color for the given object
	 * @param obj
	 * @return 
	 */
	public abstract Color getColor(Object obj);
	
	public void update() {
		//Do Nothing
	}
	
	public Map<String, Class<?>> getParameters(){
		return null;
	}

	public void setParameters( Map<String, Object> params){
		//Do Nothing
	}
	
	public boolean setParameter(String key, Object value) {
		return false;
	}

	public Object getParameterValue(String key) {
		return null;
	}
	
}
