package com.affymetrix.genometryImpl.symmetry;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SupportsCdsSpan;

/**
 * 
 * @author hiralv
 */
public interface SymSpanWithCds extends SupportsCdsSpan, SymWithProps{
	public boolean isCdsStartStopSame();
	public BioSeq getBioSeq();
	public boolean isForward();
}
