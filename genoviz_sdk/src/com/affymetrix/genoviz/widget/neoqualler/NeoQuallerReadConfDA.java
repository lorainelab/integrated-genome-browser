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

package com.affymetrix.genoviz.widget.neoqualler;

import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.datamodel.ReadConfidence;
import com.affymetrix.genoviz.util.Debug;

public class NeoQuallerReadConfDA implements DataAdapter {

  ReadConfidence trace;
  NeoQuallerI widget;

  public void setModel( Object dataModel ) {
    Debug.test(null!=dataModel, "DataAdapter: Null data model.");
    if (dataModel instanceof ReadConfidence) {
      setModel((ReadConfidence)dataModel);
    }
    else {
      throw new IllegalArgumentException("Need a ReadConfidence");
    }
  }
  protected void setModel(ReadConfidence theReadConfidence) {
    this.trace = theReadConfidence;
    if (null != this.widget) {
      widget.setReadConfidence(this.trace);
    }
  }

  public void setWidget( NeoWidgetI widget ) {
    Debug.test(null!=widget, "TraceDataAdapter: Null widget.");
    if (widget instanceof NeoQuallerI) {
      setWidget((NeoQuallerI)widget);
    }
    else {
      throw new IllegalArgumentException("Need a NeoQuallerI");
    }
  }
  protected void setWidget(NeoQuallerI widget) {
    this.widget = widget;
    if (null != this.trace) {
      widget.setReadConfidence(this.trace);
    }
  }

}
