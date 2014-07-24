package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.ID;
import com.affymetrix.genometryImpl.general.NewInstance;
import com.affymetrix.genometryImpl.general.SupportsFileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;

public interface SymmetryFilterI extends ID, SupportsFileTypeCategory, NewInstance<SymmetryFilterI> {
	public boolean filterSymmetry(BioSeq seq, SeqSymmetry sym);
}
