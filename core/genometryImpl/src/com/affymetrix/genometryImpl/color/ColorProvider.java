package com.affymetrix.genometryImpl.color;

import java.awt.Color;

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
}
