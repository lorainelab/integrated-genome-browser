package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public interface SymmetryFilterI {
	public String getName();
	public boolean setParam(Object param);
	public Object getParam();
	public boolean filterSymmetry(BioSeq seq, SeqSymmetry sym);
}
