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

package com.affymetrix.igb.genometry;

import java.util.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.symmetry.*;

/**
 *  Holds a reference to a "parent" symmetry and an index within it.
 *  All requests for properties or scores return those of the parent, and
 *  will throw exceptions if the parent is null.
 */
public class IndexedSingletonSym extends SingletonSeqSymmetry implements SymWithProps {
  int index_in_parent = -1;
  ScoredContainerSym parent = null;

  /** Constructor. Be sure to also call {@link #setParent} and {@link #setIndex}.
   */
  public IndexedSingletonSym(int start, int end, BioSeq seq)  {
    super(start, end, seq);
  }

  public void setParent(ScoredContainerSym par) { parent = par; }
  public void setIndex(int index) { index_in_parent = index; }
  public ScoredContainerSym getParent() { return parent; }
  public int getIndex() { return index_in_parent; }

  public Map getProperties() { return parent.getProperties(); }
  public Map cloneProperties() { return parent.cloneProperties(); }
  public Object getProperty(String key) { return parent.getProperty(key); }

  /** IndexedSingletonSym does not support setting properties, so this will
   *  return false.
   */
  public boolean setProperty(String key, Object val) { 
    System.err.println("IndexedSingletonSym does not support setting properties"); 
    return false;
  }

  public float[] getScores(java.util.List scorelist) {
    return this.getParent().getChildScores(this, scorelist);
  }

  public float[] getScores() {
    return this.getParent().getChildScores(this);
  }

  public int getScoreCount() { return this.getParent().getScoreCount(); }
  

}
