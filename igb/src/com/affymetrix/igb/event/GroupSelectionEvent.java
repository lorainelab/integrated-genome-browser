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

package com.affymetrix.igb.event;

import java.util.*;

public class GroupSelectionEvent extends EventObject {
  List selected_groups;

  /**
   *  Constructor.
   *  @param groups  a List of AnnotatedSeqGroup's that have been selected.
   *   (If null, will default to {@link Collections#EMPTY_LIST}.)
   */
  public GroupSelectionEvent(Object src, List groups) {
    super(src);
    this.selected_groups = groups;
    if (selected_groups == null) {
      selected_groups = Collections.EMPTY_LIST;
    }
  }
  
  /**
   *  @return a non-null List of AnnotatedSeqGroups that have been selected.
   *    The list might be empty, but will not be null.
   */
  public List getSelectedGroups() {
    return selected_groups;
  }
  
}
