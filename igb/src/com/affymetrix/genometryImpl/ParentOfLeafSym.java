package com.affymetrix.genometryImpl;

import com.affymetrix.genometry.*;

/**
 *  ParentOfLeafSym adds extra methods for efficiency, should be implemented
 *     _only_ for syms that are known to be parents of leaf syms
 *  Parents of leaf syms often lazily instantiate the leaf child syms,
 *     so ParentOfLeafSym is meant to help avoid uneccesary object creation
 *     if just want basic info from child syms
 *
 */
public interface ParentOfLeafSym extends SeqSymmetry {
  public MutableSeqSpan getChildSpan(int child_index, BioSeq seq, MutableSeqSpan return_span);
}
