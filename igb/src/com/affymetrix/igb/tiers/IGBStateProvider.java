
package com.affymetrix.igb.tiers;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphStateI;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;
import com.affymetrix.genometryImpl.style.StateProvider;

public class IGBStateProvider implements StateProvider {
  
  /** Creates a new instance of IGBStateProvider */
  public IGBStateProvider() {
  }

  public IAnnotStyleExtended getAnnotStyle(String name) {
    return AnnotStyle.getInstance(name);
  }

  public GraphStateI getGraphState(String name) {
    return new GraphState(name);
  }
  
}
