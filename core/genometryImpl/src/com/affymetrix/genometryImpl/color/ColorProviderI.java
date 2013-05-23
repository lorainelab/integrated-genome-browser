package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.general.ID;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
public interface ColorProviderI extends ID {

	/**
	 * Get color for the given object
	 * @param sym
	 * @return 
	 */
	public Color getColor(SeqSymmetry sym);
	
	public ColorProviderI clone();
}
