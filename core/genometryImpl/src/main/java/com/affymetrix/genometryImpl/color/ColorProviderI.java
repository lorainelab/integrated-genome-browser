package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.general.ID;
import com.affymetrix.genometryImpl.general.NewInstance;
import com.affymetrix.genometryImpl.general.SupportsFileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
public interface ColorProviderI extends ID, SupportsFileTypeCategory, NewInstance<ColorProviderI> {

	/**
	 * Get color for the given object
	 * @param sym
	 * @return 
	 */
	public Color getColor(SeqSymmetry sym);
}
