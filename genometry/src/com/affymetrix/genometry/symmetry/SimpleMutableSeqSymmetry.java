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

package com.affymetrix.genometry.symmetry;

import java.util.Vector;

import com.affymetrix.genometry.MutableSeqSymmetry;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SeqSymmetry;

public class SimpleMutableSeqSymmetry extends SimpleSeqSymmetry implements MutableSeqSymmetry {

  public SimpleMutableSeqSymmetry() {
    super();
  }

  public void addSpan(SeqSpan span) {
    if (spans == null) {
      spans = new Vector();
    }
    spans.addElement(span);
  }

  public void removeSpan(SeqSpan span) {
    if (spans != null) {
      spans.removeElement(span);
    }
  }
  
  public void setSpan(int index, SeqSpan span) {
    if (spans == null) {
      spans = new Vector();
    }
    spans.setElementAt(span, index);
  }

  public void addChild(SeqSymmetry sym) {
    if (children == null) {
      children = new Vector();
    }
    children.addElement(sym);
  }

  public void removeChild(SeqSymmetry sym) {
    children.removeElement(sym);
  }

  public SeqSymmetry getChild(int index) {
    if ((children == null) || (index >= children.size())) { return null; }
    else { return (SeqSymmetry)children.elementAt(index); }
  }

  public int getChildCount() {
    if (children == null) { return 0; }
    else { return children.size(); }
  }

  public void removeChildren() { children = null; }

  public void removeSpans() { spans = null; }

  public void clear() { 
    removeChildren();
    removeSpans();
  }

}
