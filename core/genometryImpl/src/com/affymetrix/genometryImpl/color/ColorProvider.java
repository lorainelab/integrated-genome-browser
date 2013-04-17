package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.parsers.TrackLineParser;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import java.awt.Color;

/**
 * A helper interface to be used when color is to extracted for each sym.
 * @author hiralv
 */
public interface ColorProvider {
	
	/**
	 * Get color for the given sym
	 * @param sym
	 * @return 
	 */
	public Color getColor(SymWithProps sym);
}
