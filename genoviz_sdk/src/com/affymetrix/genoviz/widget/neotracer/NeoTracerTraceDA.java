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

package com.affymetrix.genoviz.widget.neotracer;

import com.affymetrix.genoviz.widget.*;

import com.affymetrix.genoviz.datamodel.TraceI;
import com.affymetrix.genoviz.datamodel.Trace;
import com.affymetrix.genoviz.util.Debug;

public class NeoTracerTraceDA implements DataAdapter {

  TraceI trace;
  NeoTracerI widget;

  public void setModel( Object dataModel ) {
    Debug.test(null!=dataModel, "DataAdapter: Null data model.");
    if (dataModel instanceof TraceI) {
      setModel((TraceI)dataModel);
    }
    else {
      throw new IllegalArgumentException("Need a TraceI");
    }
  }
  protected void setModel(TraceI theTrace) {
    this.trace = theTrace;
    if (null != this.widget) {
      widget.setTrace(this.trace);
      ((Trace)widget.getTrace()).setPeaks();
    }
  }

  public void setWidget( NeoWidgetI widget ) {
    Debug.test(null!=widget, "TraceDataAdapter: Null widget.");
    if (widget instanceof NeoTracerI) {
      setWidget((NeoTracerI)widget);
    }
    else {
      throw new IllegalArgumentException("Need a NeoTracerI");
    }
  }
  protected void setWidget(NeoTracerI widget) {
    this.widget = widget;
    if (null != this.trace) {
      widget.setTrace(this.trace);
      ((Trace)widget.getTrace()).setPeaks();
    }
  }

}
