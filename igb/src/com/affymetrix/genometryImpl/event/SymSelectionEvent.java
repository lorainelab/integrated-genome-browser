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

package com.affymetrix.genometryImpl.event;

import java.util.*;

public class SymSelectionEvent extends EventObject {
  List selected_syms;

  /**
   *  Constructs a SymSelectionEvent.
   *  @param syms a List of SeqSymmetry's.  Can be empty, but should not be null.
   *   (If null, will default to {@link Collections#EMPTY_LIST}.)
   */
  public SymSelectionEvent(Object src, List syms) {
    super(src);
    if (syms == null) {
      this.selected_syms = Collections.EMPTY_LIST;
    } else {
      this.selected_syms = syms;
    }
  }
  
  /** @return a List of SeqSymmetry's.  May be empty, but will not be null.
   */
  public List getSelectedSyms() {
    return selected_syms;
  }
  
}
