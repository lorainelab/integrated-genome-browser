package com.affymetrix.genometryImpl.symmetry.impl;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SupportsCdsSpan;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;

/**
 *
 * @author hiralv
 */
public interface SymSpanWithCds extends SupportsCdsSpan, SymWithProps {

    public boolean isCdsStartStopSame();

    public BioSeq getBioSeq();

    public boolean isForward();
}
