package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.ID;
import com.affymetrix.genometryImpl.general.NewInstance;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public interface SymmetryFilterI extends ID, NewInstance<SymmetryFilterI> {
	public boolean setParam(Object param);
	public Object getParam();
	public boolean filterSymmetry(BioSeq seq, SeqSymmetry sym);
}
