/**
 *   Copyright (c) 2006 Affymetrix, Inc.
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

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import java.util.EventObject;

public class GenometryModelChangeEvent extends EventObject {
  
  EventType type;
  transient AnnotatedSeqGroup seq_group;
  
  public GenometryModelChangeEvent(GenometryModel gmodel, EventType type, AnnotatedSeqGroup group) {
    super(gmodel);
    this.type = type;
    this.seq_group = group;
  }
  
  public GenometryModel getGenometryModel() {
    return (GenometryModel) getSource();
  }
  
  public EventType getType() {
    return type;
  }
  
  public AnnotatedSeqGroup getSeqGroup() {
    return seq_group;
  }
  
  public static class EventType {
    String name;
    
    protected EventType(String name) {
      this.name = name;
    }

    public String toString() {
      return name;
    }
  }
  
  public static final EventType SEQ_GROUP_ADDED = new EventType("Seq Group Added");
  public static final EventType SEQ_GROUP_REMOVED = new EventType("Seq Group Removed");
}
