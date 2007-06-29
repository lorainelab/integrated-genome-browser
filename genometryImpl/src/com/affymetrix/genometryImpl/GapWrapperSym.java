/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.  
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.genometryImpl;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.genometry.util.SeqUtils;

/**
 * NOT YET READY.
 *  a GapWrapperSym wraps a symmetry (currently only depth=2 syms are supported)
 *   and returns its inverse (except for the external edges) sym, relative to a single seq.
 *  So for example if the original sym is a transcript with three exons, 
 *   the GapWrapperSym will be a sym that contains the two introns...
 *  Consider using SeqUtils methods instead.
 * @see SeqUtils#inverse(SeqSymmetry, BioSeq)
 */
public class GapWrapperSym implements SeqSymmetry {
  //TODO This class claims to be unfinished.  But is used by GenometryDasServlet
  SeqSymmetry original_sym;
  BioSeq original_seq;

  public GapWrapperSym(SeqSymmetry orig, BioSeq seq) {
    original_sym = orig;
    original_seq = seq;
  }

  public String getID() { 
    if (original_sym.getID() == null) { return null; }
    else { return (original_sym.getID() + "_gap"); }
  }

  public SeqSpan getSpan(BioSeq seq) { 
    SeqSpan result = null;
    if (seq == original_seq) { result = SeqUtils.getChildBounds(this, seq); }
    return result;
  }

  public boolean getSpan(BioSeq seq, MutableSeqSpan span) { 
    boolean success = false;
    if (seq == original_seq) {
      SeqSpan result = getSpan(seq);
      span.set(result.getStart(), result.getEnd(), seq);
      success = true;
    }
    return success;
  }

  public boolean getSpan(int index, MutableSeqSpan span) { 
    if (index == 0) { return getSpan(original_seq, span); }
    else { return false; }
  }

  public SeqSpan getSpan(int index) { 
    if (index == 0) { return getSpan(original_seq); }
    else { return null; }
  }

  public BioSeq getSpanSeq(int index) {  
    if (index == 0) { return original_seq; }
    else { return null; }
  }

  public SeqSymmetry getChild(int index) { 
    SeqSymmetry child = null;
    if (index < (original_sym.getChildCount()-1)) {
      int start = original_sym.getChild(index).getSpan(original_seq).getEnd();
      int end = original_sym.getChild(index+1).getSpan(original_seq).getStart();
      child = new SingletonSeqSymmetry(start, end, original_seq);
    }
    return child; 
  }

  //  public int getSpanCount() { return original_sym.getSpanCount(); }
  public int getSpanCount() { return 1; }
  public int getChildCount() { return original_sym.getChildCount()-1; }

}
