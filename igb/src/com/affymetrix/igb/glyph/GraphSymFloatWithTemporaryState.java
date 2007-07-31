package com.affymetrix.igb.glyph;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometryImpl.GraphSymFloat;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphStateI;


class GraphSymFloatWithTemporaryState extends GraphSymFloat {
  GraphStateI gstate;
  public GraphSymFloatWithTemporaryState(int[] x, float[] y, BioSeq seq) {
    super(x, y, "<unnamed>", seq);
    this.gstate = GraphState.getTemporaryGraphState();
  }

  public GraphStateI getGraphState() {
    return gstate;
  }
}