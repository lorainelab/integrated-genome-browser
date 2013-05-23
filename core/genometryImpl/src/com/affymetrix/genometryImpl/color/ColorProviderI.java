package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
public interface ColorProviderI {

	public String getName();
	
	public String getDisplay();
	
	/**
	 * Get color for the given object
	 * @param sym
	 * @return 
	 */
	public Color getColor(SeqSymmetry sym);
	
	public ColorProviderI clone();
}
