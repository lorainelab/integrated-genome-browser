package com.affymetrix.igb.genometry;

import com.affymetrix.genometry.*;

public interface IndexedSym extends SeqSymmetry {
  public void setParent(ScoredContainerSym parent);
  public ScoredContainerSym getParent();
  public void setIndex(int index);
  public int getIndex();
}
