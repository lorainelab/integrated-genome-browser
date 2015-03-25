package com.affymetrix.genometry.symmetry;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SupportsCdsSpan;

/**
 *
 * @author hiralv
 */
public interface SymSpanWithCds extends SupportsCdsSpan, SymWithProps {

    public boolean isCdsStartStopSame();

    public BioSeq getBioSeq();

    public boolean isForward();
}
