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

import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.DerivedSeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;

public class SimpleDerivedSeqSymmetry extends SimpleMutableSeqSymmetry 
  implements DerivedSeqSymmetry  {

  SeqSymmetry original_sym;

  public SimpleDerivedSeqSymmetry() {
    super();
  }

  public SimpleDerivedSeqSymmetry(SeqSymmetry sym) {
    super();
    SeqUtils.copyToDerived(sym, this);
  }
  
  public SeqSymmetry getOriginalSymmetry() {
    return original_sym;
  }

  public void setOriginalSymmetry(SeqSymmetry sym) {
    original_sym = sym;
  }
}
