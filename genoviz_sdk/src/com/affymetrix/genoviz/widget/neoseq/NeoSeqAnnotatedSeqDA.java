/**
*   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.widget.neoseq;

import com.affymetrix.genoviz.widget.*;

import com.affymetrix.genoviz.datamodel.AnnotatedSequence;
import com.affymetrix.genoviz.datamodel.SequenceI;
import com.affymetrix.genoviz.util.Debug;

/**
 * addapts a NeoSeq to an AnnotatedSequence.
 */
public class NeoSeqAnnotatedSeqDA implements DataAdapter {

  SequenceI sequence;
  NeoSeqI widget;

  public void setModel( Object dataModel ) {
    Debug.test(null!=dataModel, "DataAdapter: Null data model.");
    Debug.inform("DataAdapter loading model");
    if (dataModel instanceof AnnotatedSequence) {
      setModel((AnnotatedSequence)dataModel);
    }
    else if (dataModel instanceof SequenceI) {
      setModel((SequenceI)dataModel);
    }
    else {
      throw new IllegalArgumentException(
          "Need a AnnotatedSequence. Got a " + dataModel.getClass().getName());
    }
  }

  protected void setModel(AnnotatedSequence theSequence) {
    setModel(theSequence.getSequence());
    if (null != this.widget) {
      // Add the features as widget annotations.
    }
  }

  protected void setModel(SequenceI theSequence) {
    this.sequence = theSequence;
    if (null != this.widget && null != this.sequence) {
      // BUG! This is compensating for a bug in NeoSeq!
      //      When that bug is fixed,
      //      the prepended space below must be removed.
      //      Eric 4/21/98
      this.widget.setResidues(" " + this.sequence.getResidues());
    }
  }

  public void setWidget( NeoWidgetI widget ) {
    Debug.test(null!=widget, "DataAdapter: Null widget.");
    if (widget instanceof NeoSeqI) {
      setWidget((NeoSeqI)widget);
    }
    else {
      throw new IllegalArgumentException("Need a NeoSeqI");
    }
  }
  protected void setWidget(NeoSeqI widget) {
    this.widget = widget;
    if (null != this.sequence) {
    }
  }

}
