package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.ID;
import com.affymetrix.genometryImpl.general.NewInstance;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public interface SymmetryFilterI extends ID, NewInstance<SymmetryFilterI> {
	public boolean setParameterValue(String key, Object param);
	public Object getParameterValue(String key);
	public boolean filterSymmetry(BioSeq seq, SeqSymmetry sym);
}
