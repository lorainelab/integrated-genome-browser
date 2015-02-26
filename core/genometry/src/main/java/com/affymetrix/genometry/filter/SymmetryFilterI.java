package com.affymetrix.genometry.filter;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.general.ID;
import com.affymetrix.genometry.general.NewInstance;
import com.affymetrix.genometry.general.SupportsFileTypeCategory;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;

public interface SymmetryFilterI extends ID, SupportsFileTypeCategory, NewInstance<SymmetryFilterI> {

    public boolean filterSymmetry(BioSeq seq, SeqSymmetry sym);
}
