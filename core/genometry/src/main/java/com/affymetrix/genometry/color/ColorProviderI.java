package com.affymetrix.genometry.color;

import com.affymetrix.genometry.general.ID;
import com.affymetrix.genometry.general.NewInstance;
import com.affymetrix.genometry.general.SupportsFileTypeCategory;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
public interface ColorProviderI extends ID, SupportsFileTypeCategory, NewInstance<ColorProviderI> {

    /**
     * Get color for the given object
     *
     * @param sym
     * @return
     */
    public Color getColor(SeqSymmetry sym);
}
