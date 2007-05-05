/**
*   Copyright (c) 2005 Affymetrix, Inc.
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

package com.affymetrix.igb.event;

import java.util.EventObject;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;

/**
 *  Events used to let listeners know about changes in the 
 *  ID-to-Symmetry Mapping in an AnnotatedSeqGroup.
 */
public class SymMapChangeEvent extends EventObject {
  transient AnnotatedSeqGroup group;

  public SymMapChangeEvent(Object src, AnnotatedSeqGroup seq_group) {
    super(src);
    this.group = seq_group;
  }
  
  public AnnotatedSeqGroup getSeqGroup() {
    return group;
  }
}
